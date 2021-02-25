@file:Suppress("DEPRECATION")

package ru.ovm.genericcarsharing.ui.map

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.fragment_car_info.*
import kotlinx.android.synthetic.main.fragment_map.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.ovm.genericcarsharing.R
import ru.ovm.genericcarsharing.net.domain.Car
import ru.ovm.genericcarsharing.utils.BehaviorManager
import kotlin.concurrent.thread

class MapFragment : Fragment(R.layout.fragment_map) {

    private val vm: MapViewModel by viewModel()

    private lateinit var behavior: BottomSheetBehavior<*>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_show_random_car.setOnClickListener {
            vm.randomClick()
        }

        behavior = BottomSheetBehavior.from(sheet)
        behavior.state = BottomSheetBehavior.STATE_HIDDEN

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
        if (switch_use_fragment.isChecked) {
            findNavController().navigate(
                MapFragmentDirections.actionMapFragmentToCarInfoFragment(
                    car.id!!
                )
            )
        } else {
            showLocalSheet(car)
        }
    }

    private fun showLocalSheet(car: Car) {
        sheet.isVisible = true
        behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED

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

        val behaviorManager = BehaviorManager(
            behavior = behavior,
            halfRatio = .5f
        )

        thread {
            activity?.runOnUiThread {
                behavior.peekHeight = car_image.top

                val metrics = resources.displayMetrics
                val half = car_image.bottom.toFloat() / metrics.heightPixels
                behaviorManager.halfRatio = half

                behaviorManager.setBehaviorState(BehaviorManager.State.DEFAULT)
            }
        }

        controls.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_default -> behaviorManager.setBehaviorState(BehaviorManager.State.DEFAULT)
                R.id.radio_default_full -> behaviorManager.setBehaviorState(BehaviorManager.State.DEFAULT_FULL)
                R.id.radio_collapsed_expanded -> behaviorManager.setBehaviorState(
                    BehaviorManager.State.COLLAPSED_EXPANDED
                )
                R.id.radio_hidden_expanded -> behaviorManager.setBehaviorState(BehaviorManager.State.HIDDEN_EXPANDED)
                R.id.radio_expanded -> behaviorManager.setBehaviorState(BehaviorManager.State.EXPANDED)
            }
        }
    }
}

