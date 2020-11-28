package ru.ovm.genericcarsharing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.ovm.genericcarsharing.domain.Car
import ru.ovm.genericcarsharing.net.Api

class MainViewModel(private val api: Api) : ViewModel() {

    val cars: LiveData<List<Car>?>
        get() = _cars
    private val _cars = MutableLiveData<List<Car>?>()

    init {
        loadCars()
    }

    private fun loadCars() = viewModelScope.launch {
        try {
            val cars = api.getCars()

            withContext(Dispatchers.Main) {
                _cars.value = cars
            }
        } catch (e: Exception) {
            // TODO: 28.11.2020 proceed errors in next versions
            e.printStackTrace()
        }
    }

}