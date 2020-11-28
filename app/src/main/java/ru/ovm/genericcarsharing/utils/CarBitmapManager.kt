package ru.ovm.genericcarsharing.utils

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.annotation.DrawableRes

class CarBitmapManager(
    resources: Resources,
    @DrawableRes resId: Int
) {

    private val cars: MutableMap<Int, Bitmap> = mutableMapOf()

    private val baseBitmap: Bitmap

    init {
        baseBitmap = BitmapFactory.decodeResource(
            resources,
            resId,
            BitmapFactory.Options().apply { this.inMutable = true })

        cars[0] = baseBitmap
    }

    // у нас максимум будет 360 битмапов, зачем нам генерить их каждый раз заново?
    // а поскольку изображения машин у нас симметричные, то можно смело разделить 360 на 2))
    fun getCar(angle: Int): Bitmap {
        val a = angle % 180
        var bitmap: Bitmap? = cars[a]
        if (bitmap == null) {
            bitmap = rotateBitmap(a)
            cars[a] = bitmap
        }

        return bitmap
    }

    private fun rotateBitmap(angle: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(angle.toFloat()) }
        return Bitmap.createBitmap(
            baseBitmap,
            0,
            0,
            baseBitmap.width,
            baseBitmap.height,
            matrix,
            true
        )
    }

    fun destroy() {
        cars.values.forEach {
            it.recycle()
        }
    }

}