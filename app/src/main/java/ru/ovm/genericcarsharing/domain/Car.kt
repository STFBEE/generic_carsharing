package ru.ovm.genericcarsharing.domain

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Car(
    val id: Long?,
    val plate_number: String?,
    val name: String?,
    val color: Color?,
    val angle: Int?,
    val fuel_percentage: Int?,
    val latitude: Double?,
    val longitude: Double?,
) : Parcelable