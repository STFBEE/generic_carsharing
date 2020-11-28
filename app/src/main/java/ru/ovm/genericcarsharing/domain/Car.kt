package ru.ovm.genericcarsharing.domain

data class Car(
    val id: Long,
    val plate_number: String,
    val name: String,
    val color: String,
    val angle: Int,
    val fuel_percentage: Int,
    val latitude: Double,
    val longitude: Double,
)