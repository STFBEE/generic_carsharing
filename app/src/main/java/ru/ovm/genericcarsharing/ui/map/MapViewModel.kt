package ru.ovm.genericcarsharing.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.ovm.genericcarsharing.data.CarRepository
import ru.ovm.genericcarsharing.net.domain.Car
import kotlin.random.Random

class MapViewModel(
    private val repository: CarRepository
) : ViewModel() {

    private var cars: Map<Long, Car> = mutableMapOf()

    val showCarInfo: LiveData<Car?>
        get() = _showCarInfo
    private val _showCarInfo = MutableLiveData<Car?>()

    val needToRequestPermissions: LiveData<Boolean?>
        get() = _needToRequestPermissions
    private val _needToRequestPermissions = MutableLiveData<Boolean?>()


    init {
        loadCars()
    }

    fun carInfoShown() {
        _showCarInfo.value = null
    }

    fun permissionsRequested() {
        _needToRequestPermissions.value = null
    }

    private fun loadCars() = viewModelScope.launch {
        try {
            cars = repository.getCars()
        } catch (e: Exception) {
            // TODO: 28.11.2020 proceed errors in next versions
            e.printStackTrace()
        }
    }

    private fun showCarInfo(car: Car) {
        _showCarInfo.value = car
    }

    fun randomClick() {
        val i = Random.nextInt(cars.size)
        showCarInfo(cars.values.toList()[i])
    }
}