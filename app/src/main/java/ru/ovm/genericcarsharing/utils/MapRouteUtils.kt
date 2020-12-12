package ru.ovm.genericcarsharing.utils

import android.os.Bundle
import com.mapbox.api.directions.v5.models.DirectionsRoute

// взят отседова https://github.com/mapbox/mapbox-navigation-android/blob/master/examples/src/main/java/com/mapbox/navigation/examples/utils/Utils.java
object MapRouteUtils {
    const val PRIMARY_ROUTE_BUNDLE_KEY = "myPrimaryRouteBundleKey"

    /**
     * Used by the example activities to get a DirectionsRoute from a bundle.
     *
     * @param bundle to get the DirectionsRoute from
     * @return a DirectionsRoute or null
     */
    fun getRouteFromBundle(bundle: Bundle?): DirectionsRoute? {
        try {
            if (bundle?.containsKey(PRIMARY_ROUTE_BUNDLE_KEY) == true) {
                val routeAsJson = bundle.getString(PRIMARY_ROUTE_BUNDLE_KEY)
                return DirectionsRoute.fromJson(routeAsJson)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }
}