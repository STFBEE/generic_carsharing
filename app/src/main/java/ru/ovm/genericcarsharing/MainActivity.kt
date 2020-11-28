@file:Suppress("DEPRECATION")

package ru.ovm.genericcarsharing

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
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
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.route.NavigationMapRoute
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.ovm.genericcarsharing.domain.Car
import ru.ovm.genericcarsharing.domain.Color
import ru.ovm.genericcarsharing.utils.AllColorsCarBitmapsManager
import ru.ovm.genericcarsharing.utils.MapRouteUtils.PRIMARY_ROUTE_BUNDLE_KEY
import ru.ovm.genericcarsharing.utils.MapRouteUtils.getRouteFromBundle
import java.lang.ref.WeakReference
import java.util.*

class MainActivity : MapBoxActivity() {

    private val vm: MainViewModel by viewModel()

    private lateinit var map: MapboxMap
    private lateinit var navigation: MapboxNavigation

    private lateinit var iconFactory: IconFactory
    private lateinit var blueCarIcon: Icon
    private lateinit var blackCarIcon: Icon
    private lateinit var carBitmaps: AllColorsCarBitmapsManager

    private var navigationMapRoute: NavigationMapRoute? = null
    private var activeRoute: DirectionsRoute? = null

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    private val locationEngineCallback = MyLocationEngineCallback(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        prepareIcons()
        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        map_view.getMapAsync(this::onMapReady)
    }

    private fun prepareIcons() {
        iconFactory = IconFactory.getInstance(this)
        blueCarIcon = iconFactory.fromResource(R.drawable.car_silhouette_blue)
        blackCarIcon = iconFactory.fromResource(R.drawable.car_silhouette_black)

        // можно в константах включить поворот машин, то есть нюанс
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
                    this, getString(R.string.mapbox_access_token)
                ).build()

            navigation = MapboxNavigation(navigationOptions)
            navigation.registerLocationObserver(locationObserver)

            navigationMapRoute = NavigationMapRoute.Builder(map_view, mapboxMap, this)
                .withVanishRouteLineEnabled(true)
                .withMapboxNavigation(navigation)
                .build()

            navigation.navigationOptions.locationEngine.getLastLocation(locationEngineCallback)

            mapboxMap.setOnMarkerClickListener { marker ->
                val id = marker.title.toLong()
                vm.cars.value?.get(id)?.let { car -> showCarSheet(car) }

                val lastKnownLocation = mapboxMap.locationComponent.lastKnownLocation
                lastKnownLocation?.let { location ->
                    findRoute(
                        Point.fromLngLat(location.longitude, location.latitude),
                        Point.fromLngLat(marker.position.longitude, marker.position.latitude)
                    )
                }
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

                cars.values.forEach { car ->
                    var marker: MarkerOptions =
                        MarkerOptions().position(LatLng(car.latitude!!, car.longitude!!))

                    // а вдруг бекенд обновится и появятся новые цвета, а мы уже готовы (само собой это оптимизируется)
                    val color = car.color ?: if (Random().nextBoolean()) Color.BLUE else Color.BLACK

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
                }
            }
        }
    }

    private fun showCarSheet(car: Car) {
        car_fuel.text = getString(R.string.car_fuel_text, car.fuel_percentage)
        car_name.text = car.name
        car_number.text = car.plate_number

        val photo = when (car.name) {
            "Mercedes-Benz CLA 2019" -> "https://belkacar.ru/content/uploads/2020/10/cla-large2x11.jpg"
            "Kia RIO X-Line" -> "https://belkacar.ru/content/uploads/2020/10/rioxline-large2x11.jpg"
            "Volkswagen Polo" -> "https://belkacar.ru/content/uploads/2020/10/polo-large2x11.jpg"
            else -> "https://belkacar.ru/content/uploads/2020/10/rioxline-large2x11.jpg"
        }

        Glide.with(this)
            .load(photo)
            .into(car_image)

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // в примере говорится, что этот способ не оч, но у нас тут не хайлоад система, так что сойдет
        // восстанавливаем маршрут к машине, ес чо
        activeRoute?.let { outState.putString(PRIMARY_ROUTE_BUNDLE_KEY, it.toJson()) }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // сейвим построенный маршрут, чтобы показать его после пересоздания активити
        activeRoute = getRouteFromBundle(savedInstanceState)
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

    private class MyLocationEngineCallback(activity: MainActivity?) :
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

    private var permissionsManager: PermissionsManager = PermissionsManager(object :
        PermissionsListener {
        override fun onExplanationNeeded(permissionsToExplain: List<String>) {
            Toast.makeText(
                this@MainActivity,
                R.string.toast_location_permission_explanation,
                Toast.LENGTH_LONG
            )
                .show()
        }

        override fun onPermissionResult(granted: Boolean) {
            if (granted) {
                map.getStyle {
                    enableLocationComponent(it)
                }
            } else {
                Toast.makeText(
                    this@MainActivity,
                    R.string.toast_location_permission_not_granted,
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }

    })

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
}