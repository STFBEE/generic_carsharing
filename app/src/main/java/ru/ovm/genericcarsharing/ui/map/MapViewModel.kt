package ru.ovm.genericcarsharing.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.ovm.genericcarsharing.domain.Car
import ru.ovm.genericcarsharing.net.Api

class MapViewModel(private val api: Api) : ViewModel() {

    val cars: LiveData<Map<Long, Car>?>
        get() = _cars
    private val _cars = MutableLiveData<Map<Long, Car>?>()

    init {
        loadCars()
    }

    private fun loadCars() = viewModelScope.launch {
        try {
            val cars = api.getCars()

            withContext(Dispatchers.Main) {
                _cars.value = cars
                    .filter { it.id != null && it.latitude != null && it.longitude != null }
                    .map { car -> car.id!! to car }
                    .toMap()
            }
        } catch (e: Exception) {
            // TODO: 28.11.2020 proceed errors in next versions
            e.printStackTrace()
        }
    }

}