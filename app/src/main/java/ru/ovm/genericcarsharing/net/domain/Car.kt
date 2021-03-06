package ru.ovm.genericcarsharing.net.domain

data class Car(
    val id: Long?,
    val plate_number: String?,
    val name: String?,
    val color: Color?,
    val angle: Int?,
    val fuel_percentage: Int?,
    val latitude: Double?,
    val longitude: Double?,
)