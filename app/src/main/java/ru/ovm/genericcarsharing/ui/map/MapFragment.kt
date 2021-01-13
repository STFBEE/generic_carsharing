@file:Suppress("DEPRECATION")

package ru.ovm.genericcarsharing.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import kotlinx.android.synthetic.main.fragment_map.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.ovm.genericcarsharing.R
import ru.ovm.genericcarsharing.net.domain.Car
import ru.ovm.genericcarsharing.utils.requestLocationPermissions

class MapFragment : Fragment(R.layout.fragment_map) {

    private val vm: MapViewModel by viewModel()

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
        map_view.getMapAsync(vm::onMapReady)

        vm.showCarInfo.observe(viewLifecycleOwner) {
            it?.let {
                showCarSheet(it)
                vm.carInfoShown()
            }
        }

        vm.needToRequestPermissions.observe(viewLifecycleOwner) {
            if (it == true) {
                permissionsManager.requestLocationPermissions(this@MapFragment)
                vm.permissionsRequested()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        map_view.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun showCarSheet(car: Car) {
        findNavController().navigate(MapFragmentDirections.actionMapFragmentToCarInfoFragment(car.id!!))
    }

    private var permissionsManager: PermissionsManager = PermissionsManager(object :
        PermissionsListener {
        override fun onExplanationNeeded(permissionsToExplain: List<String>) {
            Toast.makeText(
                requireContext(),
                R.string.toast_location_permission_explanation,
                Toast.LENGTH_LONG
            ).show()
        }

        override fun onPermissionResult(granted: Boolean) {
            if (granted) {
                vm.permissionsGranted()
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

