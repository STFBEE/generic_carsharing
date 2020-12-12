package ru.ovm.genericcarsharing.ui.map

import android.location.Location
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import java.lang.ref.WeakReference

class MyLocationEngineCallback(listener: (List<Location>) -> Unit) :
    LocationEngineCallback<LocationEngineResult> {

    private val activityRef: WeakReference<(List<Location>) -> Unit> = WeakReference(listener)

    override fun onSuccess(result: LocationEngineResult) {
        activityRef.get()?.invoke(result.locations)
    }

    override fun onFailure(exception: Exception) = Unit
}