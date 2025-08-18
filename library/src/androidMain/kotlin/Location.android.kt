package com.eltonkola.ku

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual class LocationClient actual constructor() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var context: Context? = null

    actual fun initialize(context: Any?) {
        require(context is Context) { "Android implementation requires Android Context" }
        this.context = context
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    actual fun getLocation(): Flow<LocationState> = callbackFlow {
        if (!hasPermission()) {
            trySend(LocationState.PermissionDenied)
            close()
            return@callbackFlow
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10_000
        ).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(
                        LocationState.Success(
                            Location(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy,
                            altitude = location.altitude,
                            speed = location.speed,
                            bearing = location.bearing,
                            timestamp = location.time,
                            provider = location.provider
                            )
                        )
                    )
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable) {
                    trySend(LocationState.Error("Location unavailable"))
                }
            }
        }.also { locationCallback = it }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        )

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    actual fun hasPermission(): Boolean {
        return context?.let {
            ContextCompat.checkSelfPermission(
                it,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } ?: false
    }

    actual fun requestPermission() {
        (context as? Activity)?.requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    actual fun onDispose() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
        }
        locationCallback = null
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}

actual val currentPlatform: PlatformType
    get() = PlatformType.ANDROID


@Composable
actual fun getPlatformContext(): Any? = LocalContext.current