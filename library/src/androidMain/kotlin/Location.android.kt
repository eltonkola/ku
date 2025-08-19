package com.eltonkola.ku

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeoutOrNull

actual class LocationClient actual constructor() {
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var context: Context? = null
    private var isDisposed = false

    actual fun initialize(context: Any?) {
        require(context is Context) { "Android implementation requires Android Context" }
        this.context = context

        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)

        if (resultCode == ConnectionResult.SUCCESS) {
            this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        } else {
            throw IllegalStateException("Google Play Services not available")
        }
    }

    @SuppressLint("MissingPermission")
    actual fun getLocation(config: LocationConfig, retryTrigger: Int): Flow<LocationState> =
        flow {
            emit(LocationState.Loading)

            if (isDisposed) {
                emit(LocationState.Error("LocationClient is disposed"))
                return@flow
            }

            if (!hasPermission()) {
                emit(LocationState.PermissionDenied)
                return@flow
            }

            if (!isLocationServicesEnabled()) {
                emit(LocationState.Error("Location services are disabled. Please enable GPS in settings."))
                return@flow
            }

            val client = fusedLocationClient
                ?: throw IllegalStateException("LocationClient not initialized")

            try {
                if (config.singleRequest) {
                    getSingleLocation(client, config)?.let { location ->
                        emit(LocationState.Success(location))
                    } ?: emit(LocationState.Error("Unable to get current location"))
                } else {
                    getContinuousLocation(client, config).collect { state ->
                        emit(state)
                    }
                }
            } catch (e: Exception) {
                emit(LocationState.Error("Location request failed: ${e.message}"))
            }
        }
            .catch { e ->
                emit(LocationState.Error("Location flow error: ${e.message}"))
            }
            .distinctUntilChanged()

    @SuppressLint("MissingPermission")
    private suspend fun getSingleLocation(
        client: FusedLocationProviderClient,
        config: LocationConfig
    ): Location? {
        return withTimeoutOrNull(config.timeoutMs) {
            try {
                val priority = if (config.highAccuracy) Priority.PRIORITY_HIGH_ACCURACY
                else Priority.PRIORITY_BALANCED_POWER_ACCURACY

                val androidLocation = client.getCurrentLocation(priority, null).await()
                androidLocation?.toLocationData()
            } catch (e: Exception) {
                null
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getContinuousLocation(
        client: FusedLocationProviderClient,
        config: LocationConfig
    ): Flow<LocationState> = callbackFlow {
        val priority = if (config.highAccuracy) Priority.PRIORITY_HIGH_ACCURACY
        else Priority.PRIORITY_BALANCED_POWER_ACCURACY

        val locationRequest = LocationRequest.Builder(priority, config.updateIntervalMs)
            .setMinUpdateIntervalMillis(config.minUpdateIntervalMs)
            .setMaxUpdateDelayMillis(config.maxUpdateDelayMs)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (isDisposed) return
                result.lastLocation?.let { location ->
                    trySend(LocationState.Success(location.toLocationData()))
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                if (isDisposed) return
                if (!availability.isLocationAvailable) {
                    trySend(LocationState.Error("Location temporarily unavailable"))
                }
            }
        }.also { locationCallback = it }

        client.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
            .addOnFailureListener { exception ->
                trySend(LocationState.Error("Failed to start location updates: ${exception.message}"))
            }

        awaitClose { cleanup() }
    }

    private fun android.location.Location.toLocationData(): Location {
        return Location(
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            altitude = if (hasAltitude()) altitude else null,
            speed = if (hasSpeed()) speed else null,
            bearing = if (hasBearing()) bearing else null,
            timestamp = time,
            provider = provider
        )
    }

    private fun isLocationServicesEnabled(): Boolean {
        val locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return locationManager?.let {
            it.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    it.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } ?: false
    }

    actual fun hasPermission(): Boolean {
        return context?.let { ctx ->
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        } ?: false
    }

    actual fun requestPermission() {
        (context as? Activity)?.requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun cleanup() {
        locationCallback?.let { callback ->
            fusedLocationClient?.removeLocationUpdates(callback)
        }
        locationCallback = null
    }

    actual fun onDispose() {
        isDisposed = true
        cleanup()
        fusedLocationClient = null
        context = null
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}

// Extension for async operations
private suspend fun <T> Task<T>.await(): T = this.await()

actual val currentPlatform: PlatformType get() = PlatformType.ANDROID

@Composable
actual fun getPlatformContext(): Any? = LocalContext.current