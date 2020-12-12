@file:Suppress("DEPRECATION")

package ru.ovm.genericcarsharing.ui.map

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
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
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.route.NavigationMapRoute
import kotlinx.android.synthetic.main.fragment_map.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.ovm.genericcarsharing.R
import ru.ovm.genericcarsharing.domain.Car
import ru.ovm.genericcarsharing.domain.Color
import ru.ovm.genericcarsharing.utils.MapRouteUtils
import ru.ovm.genericcarsharing.utils.requestLocationPermissions
import java.util.*

class MapFragment : Fragment(R.layout.fragment_map) {

    private val vm: MapViewModel by viewModel()

    private lateinit var map: MapboxMap
    private lateinit var navigation: MapboxNavigation

    private lateinit var iconFactory: IconFactory
    private lateinit var blueCarIcon: Icon
    private lateinit var blackCarIcon: Icon

    private var navigationMapRoute: NavigationMapRoute? = null
    private var activeRoute: DirectionsRoute? = null

    private var locationEngineCallback = MyLocationEngineCallback { list -> updateLocations(list) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Mapbox.getInstance(requireContext(), getString(R.string.mapbox_access_token))
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        map_view.onCreate(savedInstanceState)

        prepareIcons()
        map_view.getMapAsync(this@MapFragment::onMapReady)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        map_view.onSaveInstanceState(outState)

        // в примере говорится, что этот способ не оч, но у нас тут не хайлоад система, так что сойдет
        // восстанавливаем маршрут к машине, ес чо
        activeRoute?.let { outState.putString(MapRouteUtils.PRIMARY_ROUTE_BUNDLE_KEY, it.toJson()) }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        // сейвим построенный маршрут, чтобы показать его после пересоздания активити
        activeRoute = MapRouteUtils.getRouteFromBundle(savedInstanceState)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun prepareIcons() {
        iconFactory = IconFactory.getInstance(requireContext())
        blueCarIcon = iconFactory.fromResource(R.drawable.car_silhouette_blue)
        blackCarIcon = iconFactory.fromResource(R.drawable.car_silhouette_black)
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        if (PermissionsManager.areLocationPermissionsGranted(requireContext())) {
            val customLocationComponentOptions = LocationComponentOptions.builder(requireContext())
                .trackingGesturesManagement(true)
                .build()

            val locationComponentActivationOptions =
                LocationComponentActivationOptions.builder(requireContext(), loadedMapStyle)
                    .locationComponentOptions(customLocationComponentOptions)
                    .build()

            map.locationComponent.apply {
                activateLocationComponent(locationComponentActivationOptions)
                isLocationComponentEnabled = true
                cameraMode = CameraMode.TRACKING
                renderMode = RenderMode.COMPASS
            }

            val navigationOptions =
                MapboxNavigation.defaultNavigationOptionsBuilder(
                    requireContext(), getString(R.string.mapbox_access_token)
                ).build()

            navigation = MapboxNavigation(navigationOptions)
            navigation.registerLocationObserver(locationObserver)

            navigationMapRoute = NavigationMapRoute.Builder(map_view, map, this)
                .withVanishRouteLineEnabled(true)
                .withMapboxNavigation(navigation)
                .build()

            navigation.navigationOptions.locationEngine.getLastLocation(locationEngineCallback)

        } else {
            permissionsManager.requestLocationPermissions(this)
        }
    }

    @SuppressLint("MissingPermission")
    private fun onMapReady(mapboxMap: MapboxMap) {
        map = mapboxMap
        val styleUri = Style.Builder().fromUri("mapbox://styles/belkacar/ckdj89h8c0rk61jlgb850lece")
        mapboxMap.setStyle(styleUri) { style ->
            enableLocationComponent(style)

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

                true
            }

            activeRoute?.let {
                val routes: List<DirectionsRoute> = listOf(it)
                navigationMapRoute?.addRoutes(routes)
                navigation.setRoutes(routes)
            }
        }

        vm.cars.observe(this) { cars ->
            cars?.values?.forEach { car ->
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

                mapboxMap.addMarker(marker)
            }
        }
    }

    private fun showCarSheet(car: Car) {
        findNavController().navigate(MapFragmentDirections.actionMapFragmentToCarInfoFragment(car))
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

    fun updateLocations(locations: List<Location>) {
        map.locationComponent.forceLocationUpdate(locations, false)
    }

    private var permissionsManager: PermissionsManager = PermissionsManager(object :
        PermissionsListener {
        override fun onExplanationNeeded(permissionsToExplain: List<String>) {
            Toast.makeText(
                requireContext(),
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
                    requireContext(),
                    R.string.toast_location_permission_not_granted,
                    Toast.LENGTH_LONG
                ).show()
                requireActivity().finish()
            }
        }
    })

    private val locationObserver: LocationObserver = object : LocationObserver {
        override fun onEnhancedLocationChanged(
            enhancedLocation: Location,
            keyPoints: List<Location>
        ) {
            if (keyPoints.isEmpty()) {
                updateLocations(listOf(enhancedLocation))
            } else {
                updateLocations(keyPoints)
            }
        }

        override fun onRawLocationChanged(rawLocation: Location) = Unit
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

    override fun onLowMemory() {
        super.onLowMemory()
        map_view.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        map_view.onDestroy()
    }
}

