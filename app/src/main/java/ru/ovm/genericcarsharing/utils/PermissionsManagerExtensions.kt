package ru.ovm.genericcarsharing.utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.fragment.app.Fragment
import com.mapbox.android.core.permissions.PermissionsManager
import java.util.*

// так как мы дергаем методы пермишенов из фрагмента, то нам нужен метод androidx.fragment.app.Fragment.requestPermissions
// иначе не сработает колбек androidx.fragment.app.Fragment.onRequestPermissionsResult

fun PermissionsManager.requestLocationPermissions(fragment: Fragment) {
    try {
        val packageInfo = fragment.requireActivity().packageManager.getPackageInfo(
            fragment.requireActivity().packageName, PackageManager.GET_PERMISSIONS
        )
        val requestedPermissions = packageInfo.requestedPermissions
        if (requestedPermissions != null) {
            val permissionList = listOf(*requestedPermissions)
            val fineLocPermission =
                permissionList.contains(Manifest.permission.ACCESS_FINE_LOCATION)
            val coarseLocPermission =
                permissionList.contains(Manifest.permission.ACCESS_COARSE_LOCATION)
            val backgroundLocPermission =
                permissionList.contains("android.permission.ACCESS_BACKGROUND_LOCATION")

            // Request location permissions
            when {
                fineLocPermission -> {
                    this.requestLocationPermissions(fragment, true, backgroundLocPermission)
                }
                coarseLocPermission -> {
                    this.requestLocationPermissions(fragment, false, backgroundLocPermission)
                }
                else -> {
                    Log.w("PermissionsManager", "Location permissions are missing")
                }
            }
        }
    } catch (exception: Exception) {
        Log.w("PermissionsManager", exception.message!!)
    }
}

private fun PermissionsManager.requestLocationPermissions(
    fragment: Fragment, requestFineLocation: Boolean,
    requestBackgroundLocation: Boolean
) {
    val permissions: MutableList<String> = ArrayList()
    if (requestFineLocation) {
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && requestBackgroundLocation) {
        permissions.add("android.permission.ACCESS_BACKGROUND_LOCATION")
    }
    this.requestPermissions(fragment, permissions.toTypedArray())
}

private fun PermissionsManager.requestPermissions(
    fragment: Fragment,
    permissions: Array<String>
) {
    val permissionsToExplain = ArrayList<String>()
    for (permission in permissions) {
        if (fragment.shouldShowRequestPermissionRationale(permission)) {
            permissionsToExplain.add(permission)
        }

        if (listener != null && permissionsToExplain.size > 0) {
            // The developer should show an explanation to the user asynchronously
            listener.onExplanationNeeded(permissionsToExplain)
        }
    }
    fragment.requestPermissions(permissions, 0)
}