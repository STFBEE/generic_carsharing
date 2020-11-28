@file:Suppress("DEPRECATION")

package ru.ovm.genericcarsharing

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.navigation.base.internal.route.RouteUrl
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.route.NavigationMapRoute
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.ovm.genericcarsharing.domain.Color
import ru.ovm.genericcarsharing.utils.AllColorsCarBitmapsManager
import ru.ovm.genericcarsharing.utils.MapRouteUtils.PRIMARY_ROUTE_BUNDLE_KEY
import ru.ovm.genericcarsharing.utils.MapRouteUtils.getRouteFromBundle
import java.lang.ref.WeakReference
import java.util.*


class MainActivity : AppCompatActivity(), PermissionsListener {

    private val vm: MainViewModel by viewModel()

    private var permissionsManager: PermissionsManager = PermissionsManager(this)
    private lateinit var map: MapboxMap
    private lateinit var navigation: MapboxNavigation

    private lateinit var blueCarIcon: Icon
    private lateinit var blackCarIcon: Icon

    private lateinit var carBitmaps: AllColorsCarBitmapsManager

    private lateinit var iconFactory: IconFactory

    private var navigationMapRoute: NavigationMapRoute? = null
    private var activeRoute: DirectionsRoute? = null
    private val mapboxReplayer = MapboxReplayer()

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))

        prepareIcons()

        setContentView(R.layout.activity_main)
        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)

        map_view.onCreate(savedInstanceState)
        map_view.getMapAsync(this::onMapReady)
    }

    private fun prepareIcons() {
        // я пробовал сделать через плагин все, но там прям жесть с классами для синхронизации и не оч понятно как рисовать иконки из ресурсов
        // + IconFactory задеприкейчен с 7.0.0, а в рекомендациях было использование 5.9.0, so...
        // implementation 'com.mapbox.mapboxsdk:mapbox-android-plugin-annotation-v9:0.9.0'
        //        val symbolManager = SymbolManager(map_view, map, map.style!!)
        //        symbolManager.create(SymbolOptions().withLatLng(LatLng()))
        iconFactory = IconFactory.getInstance(this)
        blueCarIcon = iconFactory.fromResource(R.drawable.car_silhouette_blue)
        blackCarIcon = iconFactory.fromResource(R.drawable.car_silhouette_black)

        if (App.ROTATE_CARS) {
            carBitmaps = AllColorsCarBitmapsManager(
                resources, mapOf(
                    Color.BLUE to R.drawable.car_silhouette_blue,
                    Color.BLACK to R.drawable.car_silhouette_black
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun onMapReady(mapboxMap: MapboxMap) {
        map = mapboxMap
        val styleUri = Style.Builder().fromUri("mapbox://styles/belkacar/ckdj89h8c0rk61jlgb850lece")
        mapboxMap.setStyle(styleUri) { style ->
            enableLocationComponent(style)

            val navigationOptions =
                MapboxNavigation.defaultNavigationOptionsBuilder(
                    this,
                    getString(R.string.mapbox_access_token)
                )
                    .locationEngine(ReplayLocationEngine(mapboxReplayer))
                    .build()
            navigation = MapboxNavigation(navigationOptions)
            navigation.registerLocationObserver(locationObserver)
            navigation.registerRouteProgressObserver(replayProgressObserver)
            mapboxReplayer.pushRealLocation(this, 0.0)
            mapboxReplayer.play()

            navigationMapRoute = NavigationMapRoute.Builder(map_view, mapboxMap, this)
                .withVanishRouteLineEnabled(true)
                .withMapboxNavigation(navigation)
                .build()

            navigation.navigationOptions.locationEngine.getLastLocation(locationEngineCallback)

            mapboxMap.setOnMarkerClickListener { marker ->
                val id = marker.title.toLong()
                vm.cars.value?.let { map ->
                    map[id]?.let { car ->
                        car_fuel.text = getString(R.string.car_fuel_text, car.fuel_percentage)
                        car_name.text = car.name
                        car_number.text = car.plate_number
                        Glide.with(this).load("https://loremflickr.com/300/200/car")
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(car_image);
                    }
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }

                val lastKnownLocation = mapboxMap.locationComponent.lastKnownLocation
                lastKnownLocation?.let { location ->
                    findRoute(
                        Point.fromLngLat(location.longitude, location.latitude),
                        Point.fromLngLat(marker.position.longitude, marker.position.latitude)
                    )
                }
                navigation.startTripSession()
                true
            }

            mapboxMap.addOnMapClickListener {
                hideRoute()
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

                true
            }
            activeRoute?.let {
                val routes: List<DirectionsRoute> = listOf(it)
                navigationMapRoute?.addRoutes(routes)
                navigation.setRoutes(routes)
            }
        }

        vm.cars.observe(this) { nullableCars ->
            nullableCars?.let { cars ->
                Toast.makeText(
                    this,
                    getString(R.string.toast_cars_loaded, cars.size),
                    Toast.LENGTH_SHORT
                ).show()

                val r = Random()
                val carsCoordinates = ArrayList<Point>()

                cars.values.forEach { car ->
                    var marker: MarkerOptions =
                        MarkerOptions().position(LatLng(car.latitude!!, car.longitude!!))

                    // а вдруг бекенд обновится и появятся новые цвета, а мы уже готовы
                    val color = car.color ?: if (r.nextBoolean()) Color.BLUE else Color.BLACK

                    marker = if (App.ROTATE_CARS && car.angle != null) {
                        val carBitmap = carBitmaps.getCarBitmap(color, car.angle)
                        marker.icon(iconFactory.fromBitmap(carBitmap!!))
                    } else {
                        val icon = when (color) {
                            Color.BLACK -> blackCarIcon
                            Color.BLUE -> blueCarIcon
                        }
                        marker.icon(icon)
                    }

                    marker = marker.setTitle(car.id.toString())

                    mapboxMap.addMarker(marker)

                    carsCoordinates.add(Point.fromLngLat(car.longitude, car.latitude))
                }

                // потратил некоторое время на попытки кластеризации, но чот оно начало падать с A/libc: Fatal signal 6 (SIGABRT), code -1 (SI_QUEUE), да и не надо оно по тз
                /* map.getStyle { style ->
                     val lineString = LineString.fromLngLats(carsCoordinates)
                     val featureCollection = FeatureCollection.fromFeature(Feature.fromGeometry(lineString))
                     val geoJsonSource = GeoJsonSource("cars", featureCollection, GeoJsonOptions()
                             .withCluster(true)
                             .withClusterMaxZoom(14)
                             .withClusterRadius(50))
                     style.addSource(geoJsonSource)
                     style.addImage("BLUECAR", resources.getDrawable(R.drawable.car_silhouette_blue))
                     style.addImage("BLACKCAR", resources.getDrawable(R.drawable.car_silhouette_black))

                     val unclustered = SymbolLayer("unclustered-cars", "cars")*//*.withProperties(
                        iconImage("BLUECAR"),
                        iconImage("BLACKCAR"),
                        iconAllowOverlap(true),
                        iconIgnorePlacement(true)
                     )
                     val clusters = SymbolLayer("clustered-cars", "cars")
                     it.addLayer(unclustered)
                     it.addLayer(clusters)
                 }*/
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            val customLocationComponentOptions = LocationComponentOptions.builder(this)
                .trackingGesturesManagement(true)
                .build()

            val locationComponentActivationOptions =
                LocationComponentActivationOptions.builder(this, loadedMapStyle)
                    .locationComponentOptions(customLocationComponentOptions)
                    .build()

            map.locationComponent.apply {
                activateLocationComponent(locationComponentActivationOptions)
                isLocationComponentEnabled = true
                cameraMode = CameraMode.TRACKING
                renderMode = RenderMode.COMPASS
            }
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast.makeText(this, R.string.toast_location_permission_explanation, Toast.LENGTH_LONG)
            .show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            map.getStyle {
                enableLocationComponent(it)
            }
        } else {
            Toast.makeText(
                this,
                R.string.toast_location_permission_not_granted,
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        map_view.onStart()
    }

    override fun onResume() {
        super.onResume()
        map_view.onResume()
    }

    override fun onPause() {
        super.onPause()
        map_view.onPause()
    }

    override fun onStop() {
        super.onStop()
        map_view.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        map_view.onSaveInstanceState(outState)

        // в примере говорится, что этот способ не оч, но у нас тут не хайлоад система, так что сойдет
        // восстанавливаем маршрут к машине, ес чо
        activeRoute?.let { outState.putString(PRIMARY_ROUTE_BUNDLE_KEY, it.toJson()) }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // сейвим построенный маршрут, чтобы показать его после пересоздания активити
        activeRoute = getRouteFromBundle(savedInstanceState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map_view.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        map_view.onDestroy()
    }

    private fun hideRoute() {
        navigationMapRoute?.updateRouteVisibilityTo(false)
    }

    private fun findRoute(origin: Point?, destination: Point?) {
        val routeOptions = RouteOptions.builder()
            .baseUrl(RouteUrl.BASE_URL)
            .user(RouteUrl.PROFILE_DEFAULT_USER)
            .profile(RouteUrl.PROFILE_WALKING)
            .geometries(RouteUrl.GEOMETRY_POLYLINE6)
            .requestUuid("")
            .accessToken(getString(R.string.mapbox_access_token))
            .coordinates(listOf(origin, destination))
            .alternatives(false)
            .build()
        navigation.requestRoutes(
            routeOptions,
            routesReqCallback
        )
    }

    private val routesReqCallback: RoutesRequestCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()) {
                activeRoute = routes[0]
                navigationMapRoute?.addRoutes(routes)
            }
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) = Unit
        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) = Unit
    }

    private val locationEngineCallback = MyLocationEngineCallback(this)

    private inner class MyLocationEngineCallback(activity: MainActivity?) :
        LocationEngineCallback<LocationEngineResult> {

        private val activityRef: WeakReference<MainActivity> = WeakReference(activity)

        override fun onSuccess(result: LocationEngineResult) {
            activityRef.get()?.updateLocation(result.locations)
        }

        override fun onFailure(exception: Exception) = Unit
    }

    fun updateLocation(location: Location) = updateLocation(listOf(location))

    fun updateLocation(locations: List<Location>) =
        map.locationComponent.forceLocationUpdate(locations, false)

    private val locationObserver: LocationObserver = object : LocationObserver {
        override fun onEnhancedLocationChanged(
            enhancedLocation: Location,
            keyPoints: List<Location>
        ) {
            if (keyPoints.isEmpty()) {
                updateLocation(enhancedLocation)
            } else {
                updateLocation(keyPoints)
            }
        }

        override fun onRawLocationChanged(rawLocation: Location) = Unit
    }

    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)
}