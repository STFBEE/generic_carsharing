package ru.ovm.genericcarsharing.net.directions

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiDirections {

    @GET("mapbox/cycling/{len_start},{lat_start};{len_end},{lat_end}?")
    suspend fun getRoute(
        @Query("token") token: String,
        @Query("geometries") geometries: String = "geojson",
        @Path("len_start") len_start: Double,
        @Path("lat_start") lat_start: Double,
        @Path("len_end") len_end: Double,
        @Path("lat_end") lat_end: Double,
    ) {

    }

    companion object {
        const val ENDPOINT: String = "https://api.mapbox.com/directions/v5/"
    }
}