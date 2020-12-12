package ru.ovm.genericcarsharing.ui.carinfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_car_info.*
import ru.ovm.genericcarsharing.R

class CarInfoFragment : BottomSheetDialogFragment() {

    private val args: CarInfoFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_car_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val car = args.car

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