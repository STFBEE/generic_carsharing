package ru.ovm.genericcarsharing.data

import ru.ovm.genericcarsharing.net.domain.Car

class CarStorage {

    private val cars: MutableMap<Long, Car> = mutableMapOf()

    suspend fun insert(car: Car): Boolean {
        if (car.id == null) return false
        cars[car.id] = car
        return true
    }

    suspend fun get(id: Long): Car? = cars[id]

    suspend fun getAllCars(): Map<Long, Car> = cars
}