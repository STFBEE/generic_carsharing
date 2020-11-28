@file:Suppress("DEPRECATION")

package ru.ovm.genericcarsharing

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
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
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.ovm.genericcarsharing.domain.Color
import ru.ovm.genericcarsharing.utils.AllColorsCarBitmapsManager
import java.util.*

class MainActivity : AppCompatActivity(), PermissionsListener {

    private val vm: MainViewModel by viewModel()

    private var permissionsManager: PermissionsManager = PermissionsManager(this)
    private lateinit var map: MapboxMap

    private lateinit var blueCarIcon: Icon
    private lateinit var blackCarIcon: Icon

    private lateinit var carBitmaps: AllColorsCarBitmapsManager

    private lateinit var iconFactory: IconFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))

        prepareIcons()

        setContentView(R.layout.activity_main)

        map_view.onCreate(savedInstanceState)
        map_view.getMapAsync(this::onMapReady)

        vm.cars.observe(this) {
            it?.let {
                Toast.makeText(this, getString(R.string.toast_cars_loaded, it.size), Toast.LENGTH_SHORT).show()

                val r = Random()

                it.forEach { car ->
                    if (car.latitude == null && car.longitude == null) {
                        // не ну это бан
                    } else {
                        var marker: MarkerOptions = MarkerOptions().position(LatLng(car.latitude!!, car.longitude!!))

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


                        if (car.plate_number != null) {
                            marker = marker.setTitle(car.plate_number)
                        }

                        if (car.name != null) {
                            marker = marker.setSnippet(car.name)
                        }

                        map.addMarker(marker)
                    }
                }
            }
        }


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
            carBitmaps = AllColorsCarBitmapsManager(resources, mapOf(
                    Color.BLUE to R.drawable.car_silhouette_blue,
                    Color.BLACK to R.drawable.car_silhouette_black
            ))
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
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map_view.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        map_view.onDestroy()
    }

    private fun onMapReady(map: MapboxMap) {
        this.map = map
        map.setStyle(Style.Builder().fromUri("mapbox://styles/belkacar/ckdj89h8c0rk61jlgb850lece")) {
            enableLocationComponent(it)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            val customLocationComponentOptions = LocationComponentOptions.builder(this)
                    .trackingGesturesManagement(true)
                    .build()

            val locationComponentActivationOptions = LocationComponentActivationOptions.builder(this, loadedMapStyle)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast.makeText(this, R.string.toast_location_permission_explanation, Toast.LENGTH_LONG).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            // может упасть, если стиль будет долго грузится, а юзер прокликает разрешение на пермишены быстрее, но да ладно
            enableLocationComponent(map.style!!)
        } else {
            Toast.makeText(this, R.string.toast_location_permission_not_granted, Toast.LENGTH_LONG).show()
            finish()
        }
    }
}