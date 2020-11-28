package ru.ovm.genericcarsharing.utils

import android.content.res.Resources
import android.graphics.Bitmap
import ru.ovm.genericcarsharing.domain.Color

// ого ет чо, фабрика фабрик?
class AllColorsCarBitmapsManager(
    resources: Resources,
    resIds: Map<Color, Int>
) {

    private val managersMap: MutableMap<Color, CarBitmapManager> = mutableMapOf()

    init {
        resIds.entries.forEach {
            managersMap[it.key] = CarBitmapManager(resources, it.value)
        }
    }

    fun getCarBitmap(color: Color, angle: Int): Bitmap? {
        return managersMap[color]?.getCar(angle)
    }

    fun destroy() {
        managersMap.values.forEach { it.destroy() }
    }

}