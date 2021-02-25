package ru.ovm.genericcarsharing.ui.carinfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_car_info.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.ovm.genericcarsharing.R
import ru.ovm.genericcarsharing.utils.BehaviorManager
import kotlin.concurrent.thread

class CarInfoFragment : BottomSheetDialogFragment() {

    private val vm: CarInfoViewModel by viewModel()
    private val args: CarInfoFragmentArgs by navArgs()

    private lateinit var behavior: BottomSheetBehavior<FrameLayout>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_car_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm.loadCar(args.carId)

        vm.car.observe(viewLifecycleOwner) {

            it?.let { car ->

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
            }
        }

        behavior = (dialog as BottomSheetDialog).behavior
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
                R.id.radio_collapsed_expanded -> behaviorManager.setBehaviorState(BehaviorManager.State.COLLAPSED_EXPANDED)
                R.id.radio_hidden_expanded -> behaviorManager.setBehaviorState(BehaviorManager.State.HIDDEN_EXPANDED)
                R.id.radio_expanded -> behaviorManager.setBehaviorState(BehaviorManager.State.EXPANDED)
            }
        }
    }
}