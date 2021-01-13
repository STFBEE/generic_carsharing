package ru.ovm.genericcarsharing.ui.carinfo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.ovm.genericcarsharing.data.CarStorage
import ru.ovm.genericcarsharing.net.domain.Car

class CarInfoViewModel(
    private val carStorage: CarStorage,
) : ViewModel() {

    val car: LiveData<Car?>
        get() = _car
    private val _car: MutableLiveData<Car?> = MutableLiveData()

    fun loadCar(id: Long) = viewModelScope.launch {
        _car.value = carStorage.get(id)
    }

}