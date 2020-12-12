package ru.ovm.genericcarsharing.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.PointF
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.DirectionsCriteria.GEOMETRY_POLYLINE
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property.LINE_CAP_ROUND
import com.mapbox.mapboxsdk.style.layers.Property.LINE_JOIN_ROUND
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.ovm.genericcarsharing.R
import ru.ovm.genericcarsharing.net.ApiCars
import ru.ovm.genericcarsharing.net.domain.Car

class MapViewModel(
    private val apiCars: ApiCars,
    private val androidContext: Context,
) : ViewModel() {

    private val cars: MutableMap<Long, Car> = mutableMapOf()
    private val carFeatures: MutableList<Feature> = mutableListOf()

    val showCarInfo: LiveData<Car?>
        get() = _showCarInfo
    private val _showCarInfo = MutableLiveData<Car?>()

    val needToRequestPermissions: LiveData<Boolean?>
        get() = _needToRequestPermissions
    private val _needToRequestPermissions = MutableLiveData<Boolean?>()

    private lateinit var map: MapboxMap

    init {
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

    private fun loadCars() = viewModelScope.launch {
        try {
            apiCars.getCars()
                .filter { it.id != null && it.latitude != null && it.longitude != null }
                .map { car -> car.id!! to car }
                .toMap(cars)

            convertCarsToFeatures()
        } catch (e: Exception) {
            // TODO: 28.11.2020 proceed errors in next versions
            e.printStackTrace()
        }
    }

    private fun showCarInfo(car: Car) {
        _showCarInfo.value = car
    }

    private fun makeRoute(car: Car) = viewModelScope.launch {
        val lastKnownLocation = map.locationComponent.lastKnownLocation ?: return@launch

        val mapboxDirectionsClient = MapboxDirections.builder()
            .origin(Point.fromLngLat(lastKnownLocation.longitude, lastKnownLocation.latitude))
            .destination(Point.fromLngLat(car.longitude!!, car.latitude!!))
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .profile(DirectionsCriteria.PROFILE_WALKING)
            .geometries(GEOMETRY_POLYLINE)
            .alternatives(true)
            .steps(true)
            .accessToken(androidContext.getString(R.string.mapbox_access_token))
            .build()

        mapboxDirectionsClient.enqueueCall(object : Callback<DirectionsResponse?> {
            override fun onResponse(
                call: Call<DirectionsResponse?>,
                response: Response<DirectionsResponse?>
            ) {
                val body = response.body()
                if (body == null || body.routes().isNullOrEmpty()) {
                    // TODO: 12.12.2020
                    return
                }
                val route = body.routes().first()
                val steps = route.legs()?.first()?.steps()

                steps?.map {
                    val lineStringRepresentingSingleStep =
                        LineString.fromPolyline(it.geometry()!!, Constants.PRECISION_5)
                    Feature.fromGeometry(lineStringRepresentingSingleStep)
                }?.let { features ->
                    map.style?.getSourceAs<GeoJsonSource?>(SOURCE_ROUTE)?.setGeoJson(
                        FeatureCollection.fromFeatures(features)
                    )
                }
            }

            override fun onFailure(call: Call<DirectionsResponse?>, t: Throwable) {
                //TODO("Not yet implemented")
            }
        })
    }

    private fun onStyleLoaded(style: Style) {

        style.addSource(GeoJsonSource(SOURCE_ROUTE))
        style.addSource(GeoJsonSource(SOURCE_CARS))

        setUpCarImages(style)
        setUpRouteLayer(style)
        setUpCarsIconLayer(style)

        updateMapContent()
        enableLocationComponent(style)

        map.addOnMapClickListener {
            val handled = handleIconClick(map.projection.toScreenLocation(it))
            if (!handled) {
                hideRoute()
            }

            true
        }
    }

    private fun handleIconClick(point: PointF): Boolean {
        val features: List<Feature> = map.queryRenderedFeatures(point, LAYER_CARS)
        features.firstOrNull()?.let { feature ->
            val carId = feature.getNumberProperty(PROPERTY_CAR_ID).toLong()
            cars[carId]?.let {
                showCarInfo(it)
                makeRoute(it)
                return true
            }
        }
        return false
    }

    private fun setUpCarImages(style: Style) {
        style.addImage(
            IMAGE_CAR_BLACK_ID, BitmapFactory.decodeResource(
                androidContext.resources, R.drawable.car_silhouette_black
            )
        )
        style.addImage(
            IMAGE_CAR_BLUE_ID, BitmapFactory.decodeResource(
                androidContext.resources, R.drawable.car_silhouette_blue
            )
        )
    }

    private fun setUpCarsIconLayer(style: Style) {
        style.addLayer(
            SymbolLayer(LAYER_CARS, SOURCE_CARS).withProperties(
                iconImage(IMAGE_CAR_BLACK_ID), // TODO: 12.12.2020 expression for color select
                iconSize(.75f),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
            )
        )
    }

    private fun setUpRouteLayer(style: Style) {
        style.addLayer(
            LineLayer(LAYER_ROUTE, SOURCE_ROUTE).withProperties(
                lineWidth(5f),
                lineCap(LINE_CAP_ROUND),
                lineJoin(LINE_JOIN_ROUND),
                lineColor(androidContext.getColor(R.color.belka_blue))
            )
        )
    }

    private fun convertCarsToFeatures() {
        carFeatures.clear()

        for (car in cars.values) {
            if (car.longitude == null || car.latitude == null || car.id == null) continue

            val singleFeature = Feature.fromGeometry(
                Point.fromLngLat(car.longitude, car.latitude)
            )
            singleFeature.addNumberProperty(PROPERTY_CAR_ID, car.id)
            singleFeature.addStringProperty(PROPERTY_CAR_COLOR, car.color.toString())
            carFeatures.add(singleFeature)
        }

        updateMapContent()
    }

    private fun updateMapContent() {
        map.style?.getSourceAs<GeoJsonSource?>(SOURCE_CARS)?.setGeoJson(
            FeatureCollection.fromFeatures(carFeatures)
        )
    }

    private fun hideRoute() {
        map.style?.getSourceAs<GeoJsonSource?>(SOURCE_ROUTE)?.setGeoJson(
            FeatureCollection.fromFeatures(emptyList())
        )
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
        } else {
            _needToRequestPermissions.value = true
        }
    }

    companion object {
        const val IMAGE_CAR_BLACK_ID = "IMAGE_CAR_BLACK_ID"
        const val IMAGE_CAR_BLUE_ID = "IMAGE_CAR_BLUE_ID"
        const val LAYER_CARS = "LAYER_CARS"
        const val LAYER_ROUTE = "LAYER_ROUTE"
        const val SOURCE_CARS = "SOURCE_CARS"
        const val SOURCE_ROUTE = "SOURCE_ROUTE"
        const val CAR_ANGLE = "CAR_ANGLE"
        const val PROPERTY_CAR_ID = "PROPERTY_CAR_ID"
        const val PROPERTY_CAR_COLOR = "PROPERTY_CAR_COLOR"
    }
}