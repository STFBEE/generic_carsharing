@file:Suppress("DEPRECATION")

package ru.ovm.genericcarsharing.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.launch
import ru.ovm.genericcarsharing.R
import ru.ovm.genericcarsharing.net.cars.ApiCars
import ru.ovm.genericcarsharing.net.cars.domain.Car
import ru.ovm.genericcarsharing.net.cars.domain.Color
import ru.ovm.genericcarsharing.net.directions.ApiDirections
import java.lang.ref.WeakReference
import java.util.*

class MapViewModel(
    private val apiCars: ApiCars,
    private val apiDirections: ApiDirections,
    private val androidContext: Context,
) : ViewModel() {

    var cars: MutableMap<Long, Car> = mutableMapOf()
    var markers: MutableList<MarkerOptions> = mutableListOf()

    val showCarInfo: LiveData<Car?>
        get() = _showCarInfo
    private val _showCarInfo = MutableLiveData<Car?>()

    val currentLocation: LiveData<List<Location>?>
        get() = _currentLocation
    private val _currentLocation: MutableLiveData<List<Location>?> = MutableLiveData()

    val needToRequestPermissions: LiveData<Boolean?>
        get() = _needToRequestPermissions
    private val _needToRequestPermissions = MutableLiveData<Boolean?>()

    val needToUpdateMapRoute: LiveData<Pair<MapboxMap, MapboxNavigation>?>
        get() = _needToUpdateMapRoute
    private val _needToUpdateMapRoute = MutableLiveData<Pair<MapboxMap, MapboxNavigation>?>()

    private lateinit var map: MapboxMap
    private lateinit var navigation: MapboxNavigation

    private lateinit var iconFactory: IconFactory
    private lateinit var blueCarIcon: Icon
    private lateinit var blackCarIcon: Icon

    private var navigationMapRouteHolder: WeakReference<NavigationMapRoute> = WeakReference(null)
    private var activeRoute: DirectionsRoute? = null

    init {
        prepareIcons()
        loadCars()
    }

    fun onMapReady(mapboxMap: MapboxMap) {
        map = mapboxMap
        val styleUri = Style.Builder().fromUri("mapbox://styles/belkacar/ckdj89h8c0rk61jlgb850lece")
        mapboxMap.setStyle(styleUri, ::onStyleLoaded)
    }

    fun carInfoShown() {
        _showCarInfo.value = null
    }

    fun permissionsRequested() {
        _needToRequestPermissions.value = null
    }

    fun permissionsGranted() {
        map.getStyle {
            enableLocationComponent(it)
        }
    }

    fun updateMapRoute(mapRoute: NavigationMapRoute?) {
        // это не очень хорошо
        navigationMapRouteHolder = WeakReference(mapRoute)
        _needToUpdateMapRoute.value = null
    }

    private fun prepareIcons() {
        iconFactory = IconFactory.getInstance(androidContext)
        blueCarIcon = iconFactory.fromResource(R.drawable.car_silhouette_blue)
        blackCarIcon = iconFactory.fromResource(R.drawable.car_silhouette_black)
    }

    private fun loadCars() = viewModelScope.launch {
        try {
            apiCars.getCars()
                .filter { it.id != null && it.latitude != null && it.longitude != null }
                .map { car -> car.id!! to car }
                .toMap(cars)
            carsToMarkers()
        } catch (e: Exception) {
            // TODO: 28.11.2020 proceed errors in next versions
            e.printStackTrace()
        }
    }

    private fun showCarInfo(car: Car) {
        _showCarInfo.value = car
    }

    private fun onStyleLoaded(style: Style) {
        enableLocationComponent(style)

        map.setOnMarkerClickListener { marker ->
            val id = marker.title.toLong()
            cars[id]?.let { car -> showCarInfo(car) }

            val lastKnownLocation = map.locationComponent.lastKnownLocation
            lastKnownLocation?.let { location ->
                findRoute(
                    Point.fromLngLat(location.longitude, location.latitude),
                    Point.fromLngLat(marker.position.longitude, marker.position.latitude)
                )
            }
            true
        }

        map.addOnMapClickListener {
            hideRoute()

            true
        }

        activeRoute?.let {
            val routes: List<DirectionsRoute> = listOf(it)
            navigationMapRouteHolder.get()?.addRoutes(routes)
            navigation.setRoutes(routes)
        }
    }

    private fun carsToMarkers() {
        markers.clear()
        cars.values.forEach { car ->
            // а вдруг бекенд обновится и появятся новые цвета, а мы уже готовы (само собой это оптимизируется)
            val color = car.color ?: if (Random().nextBoolean()) Color.BLUE else Color.BLACK
            val marker: MarkerOptions = MarkerOptions()
                .position(LatLng(car.latitude!!, car.longitude!!))
                .icon(
                    when (color) {
                        Color.BLACK -> blackCarIcon
                        Color.BLUE -> blueCarIcon
                    }
                )
                .setTitle(car.id.toString())
            markers.add(marker)
        }
        updateMapContent()
    }

    private fun updateMapContent() {
        map.clear()
        map.addMarkers(markers)
    }

    private fun findRoute(origin: Point?, destination: Point?) {
        val routeOptions = RouteOptions.builder()
            .baseUrl(RouteUrl.BASE_URL)
            .user(RouteUrl.PROFILE_DEFAULT_USER)
            .profile(RouteUrl.PROFILE_WALKING)
            .geometries(RouteUrl.GEOMETRY_POLYLINE6)
            .requestUuid("")
            .accessToken(androidContext.getString(R.string.mapbox_access_token))
            .coordinates(listOf(origin, destination))
            .alternatives(false)
            .build()
        navigation.requestRoutes(
            routeOptions,
            routesReqCallback
        )
    }

    private fun hideRoute() {
        navigationMapRouteHolder.get()?.updateRouteVisibilityTo(false)
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        if (PermissionsManager.areLocationPermissionsGranted(androidContext)) {
            val customLocationComponentOptions = LocationComponentOptions.builder(androidContext)
                .trackingGesturesManagement(true)
                .build()

            val locationComponentActivationOptions =
                LocationComponentActivationOptions.builder(androidContext, loadedMapStyle)
                    .locationComponentOptions(customLocationComponentOptions)
                    .build()

            map.locationComponent.apply {
                activateLocationComponent(locationComponentActivationOptions)
                isLocationComponentEnabled = true
                cameraMode = CameraMode.TRACKING
                renderMode = RenderMode.COMPASS
            }

            val navigationOptions = MapboxNavigation.defaultNavigationOptionsBuilder(
                androidContext,
                androidContext.getString(R.string.mapbox_access_token)
            ).build()

            navigation = MapboxNavigation(navigationOptions)
            navigation.registerLocationObserver(locationObserver)


            navigation.navigationOptions.locationEngine.getLastLocation(locationEngineCallback)

            _needToUpdateMapRoute.value = Pair(map, navigation)
        } else {
            _needToRequestPermissions.value = true
        }
    }

    private var locationEngineCallback = MyLocationEngineCallback { list ->
        map.locationComponent.forceLocationUpdate(list, false)
    }

    private val locationObserver: LocationObserver = object : LocationObserver {
        override fun onEnhancedLocationChanged(
            enhancedLocation: Location,
            keyPoints: List<Location>
        ) {
            if (keyPoints.isEmpty()) {
                _currentLocation.value = listOf(enhancedLocation)
            } else {
                _currentLocation.value = keyPoints
            }
        }

        override fun onRawLocationChanged(rawLocation: Location) = Unit
    }

    private val routesReqCallback: RoutesRequestCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()) {
                activeRoute = routes[0]
                navigationMapRouteHolder.get()?.addRoutes(routes)
            }
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) = Unit
        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) = Unit
    }

}