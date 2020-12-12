package ru.ovm.genericcarsharing.net

import retrofit2.http.GET
import ru.ovm.genericcarsharing.net.domain.Car

interface ApiCars {
    @GET("/Gary111/TrashCan/master/2000_cars.json")
    suspend fun getCars(): List<Car>

    companion object {
        const val ENDPOINT: String = "https://raw.githubusercontent.com"
    }
}