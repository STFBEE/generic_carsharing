@file:Suppress("DEPRECATION")

package ru.ovm.genericcarsharing.ui.map

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_map.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.ovm.genericcarsharing.R
import ru.ovm.genericcarsharing.net.domain.Car

class MapFragment : Fragment(R.layout.fragment_map) {

    private val vm: MapViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        random_car_show.setOnClickListener {
            vm.randomClick()
        }

        vm.showCarInfo.observe(viewLifecycleOwner) {
            it?.let {
                showCarSheet(it)
                vm.carInfoShown()
            }
        }

        vm.needToRequestPermissions.observe(viewLifecycleOwner) {
            if (it == true) {
                vm.permissionsRequested()
            }
        }
    }

    private fun showCarSheet(car: Car) {
        findNavController().navigate(MapFragmentDirections.actionMapFragmentToCarInfoFragment(car.id!!))
    }
}

