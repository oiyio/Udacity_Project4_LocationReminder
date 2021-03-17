package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.locationreminders.savereminder.checkDeviceLocationSettings

val runningQOrLater = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

/* 🔥 Checks if the following 2 permissions are granted :
* - foreground location
* - background location
* */
@TargetApi(29)
fun foregroundAndBackgroundLocationPermissionGranted(context: Context): Boolean {
    return isForegroundLocationPermissionGranted(context) && isBackgroundLocationPermissionGranted(context)
}

/* 🔥 Checks if the following permission is granted :
* - foreground location
* */
fun isForegroundLocationPermissionGranted(context: Context): Boolean {
    return (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION))
}

/*
* 🔥 Checks if the following permission is granted :
* - background location
* */
@TargetApi(29)
fun isBackgroundLocationPermissionGranted(context: Context): Boolean {
    return if (runningQOrLater) {
        PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else {
        true
    }
}


// 🔥 Requests the permission of "foreground location ( ACCESS_FINE_LOCATION )"
fun requestForegroundLocationPermission(fragment: Fragment) {
    if (isForegroundLocationPermissionGranted(fragment.requireContext())) {
        return
    }

    val permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    val resultCode = REQUEST_FOREGROUND_ONLY_PERMISSION_REQUEST_CODE

    fragment.requestPermissions(
            permissionsArray,
            resultCode
    )
}

/*
* 🔥 Requests the following permissions :
* - foreground location ( ACCESS_FINE_LOCATION )
* - on Android 10+ (Q), ACCESS_BACKGROUND_LOCATION
* */
// 🔥 Requests the permission of "background location ( ACCESS_BACKGROUND_LOCATION )"
@TargetApi(29)
fun requestBackgroundLocationPermission(fragment: Fragment) {
    if (isBackgroundLocationPermissionGranted(fragment.requireContext())) {
        return
    }

    val permissionsArray = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    val resultCode = REQUEST_BACKGROUND_ONLY_PERMISSION_REQUEST_CODE

    if (isForegroundLocationPermissionGranted(fragment.requireContext())) {
        if (runningQOrLater) {
            fragment.requestPermissions(
                    permissionsArray,
                    resultCode
            )
        }
    }
}

fun isPermissionGranted(context: Context, permission: String): Boolean {
    return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

fun showSnackbarWithSettingsAction(activity: FragmentActivity) {
    Snackbar.make(activity.findViewById(android.R.id.content), R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.settings) {
                // Launches App settings screen.
                activity.startActivity(
                        Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
            }.show()
}

const val REQUEST_FOREGROUND_ONLY_PERMISSION_REQUEST_CODE = 34
const val REQUEST_BACKGROUND_ONLY_PERMISSION_REQUEST_CODE = 35
const val REQUEST_TURN_DEVICE_LOCATION_ON = 29