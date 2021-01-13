package ru.ovm.genericcarsharing.data

import ru.ovm.genericcarsharing.net.ApiCars
import ru.ovm.genericcarsharing.net.domain.Car

class CarRepository(
    private val carStorage: CarStorage,
    private val apiCars: ApiCars,
) {
    suspend fun loadCars() {
        // TODO: 13.01.2021 тут проверим, что есть в локале

        val cars = apiCars.getCars()
            .filter { it.id != null && it.latitude != null && it.longitude != null }
            .map { car -> car.id!! to car }
            .toMap()

        cars.forEach { carStorage.insert(it.value) }
    }

    suspend fun getCar(id: Long): Car? {
        val localCar = carStorage.get(id)
        if (localCar == null) {
            // TODO: 13.01.2021 в кеше не нашли, грузим с инета
        }
        return localCar
    }

    suspend fun getCars(): Map<Long, Car> {
        loadCars()
        return carStorage.getAllCars()
    }

}