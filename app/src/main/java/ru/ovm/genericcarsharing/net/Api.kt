package ru.ovm.genericcarsharing.net

import retrofit2.http.GET
import ru.ovm.genericcarsharing.domain.Car

interface Api {
    @GET("/Gary111/TrashCan/master/2000_cars.json")
    suspend fun getCars(): List<Car>

    companion object {
        val URL: String = "https://raw.githubusercontent.com"
    }
}