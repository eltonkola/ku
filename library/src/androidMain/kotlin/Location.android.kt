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
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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
                    getSingleLocationWithFallback(client, config)?.let { location ->
                        emit(LocationState.Success(location))
                    } ?: emit(LocationState.Error("Unable to get current location. Please try again or ensure you're in an area with good GPS signal."))
                } else {
                    var hasEverEmittedLocation = false
                    var lastSuccessfulLocation: Location? = null

                    getContinuousLocation(client, config).collect { state ->
                        when (state) {
                            is LocationState.Success -> {
                                hasEverEmittedLocation = true
                                lastSuccessfulLocation = state.location
                                emit(state)
                            }
                            is LocationState.Error -> {
                                // Only emit error if we never had a successful location
                                if (!hasEverEmittedLocation) {
                                    emit(state)
                                }
                                // Otherwise, silently ignore the error and keep showing last location
                            }
                            else -> emit(state)
                        }
                    }
                }
            } catch (e: SecurityException) {
                emit(LocationState.PermissionDenied)
            } catch (e: Exception) {
                emit(LocationState.Error("Location request failed: ${e.message}"))
            }
        }
            .catch { e ->
                emit(LocationState.Error("Location flow error: ${e.message}"))
            }
            .distinctUntilChanged()

    @SuppressLint("MissingPermission")
    private suspend fun getSingleLocationWithFallback(
        client: FusedLocationProviderClient,
        config: LocationConfig
    ): Location? {
        return withTimeoutOrNull(config.timeoutMs) {
            try {
                // First, try to get current location with cancellation token
                val cancellationTokenSource = CancellationTokenSource()
                val priority = if (config.highAccuracy) Priority.PRIORITY_HIGH_ACCURACY
                else Priority.PRIORITY_BALANCED_POWER_ACCURACY

                val currentLocation = client.getCurrentLocation(priority, cancellationTokenSource.token).await()

                // If getCurrentLocation returns null, try alternative methods
                if (currentLocation != null) {
                    return@withTimeoutOrNull currentLocation.toLocationData()
                }

                // Fallback 1: Try last known location
                val lastKnownLocation = client.lastLocation.await()
                if (lastKnownLocation != null && isLocationRecent(lastKnownLocation)) {
                    return@withTimeoutOrNull lastKnownLocation.toLocationData()
                }

                // Fallback 2: Use location updates for a short period
                return@withTimeoutOrNull getLocationFromUpdates(client, config)

            } catch (e: SecurityException) {
                throw e // Re-throw security exceptions
            } catch (e: Exception) {
                // Try fallback methods even if getCurrentLocation throws
                try {
                    val lastKnownLocation = client.lastLocation.await()
                    if (lastKnownLocation != null && isLocationRecent(lastKnownLocation)) {
                        return@withTimeoutOrNull lastKnownLocation.toLocationData()
                    }

                    return@withTimeoutOrNull getLocationFromUpdates(client, config)
                } catch (fallbackException: Exception) {
                    null
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLocationFromUpdates(
        client: FusedLocationProviderClient,
        config: LocationConfig
    ): Location? = suspendCancellableCoroutine { continuation ->
        val priority = if (config.highAccuracy) Priority.PRIORITY_HIGH_ACCURACY
        else Priority.PRIORITY_BALANCED_POWER_ACCURACY

        val locationRequest = LocationRequest.Builder(priority, 1000L) // Fast updates for single location
            .setMinUpdateIntervalMillis(500L)
            .setMaxUpdateDelayMillis(2000L)
            .setMaxUpdates(1) // Stop after first location
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    if (continuation.isActive) {
                        continuation.resume(location.toLocationData())
                    }
                }
                // Clean up
                client.removeLocationUpdates(this)
            }
        }

        // Set up cancellation
        continuation.invokeOnCancellation {
            client.removeLocationUpdates(callback)
        }

        // Start location updates
        client.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
            .addOnFailureListener { exception ->
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
    }

    private fun isLocationRecent(location: android.location.Location): Boolean {
        val maxAgeMs = 5 * 60 * 1000 // 5 minutes
        return (System.currentTimeMillis() - location.time) < maxAgeMs
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

        var lastSuccessfulLocation: Location? = null
        var hasEverReceivedLocation = false

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (isDisposed) return
                result.lastLocation?.let { location ->
                    val locationData = location.toLocationData()
                    lastSuccessfulLocation = locationData
                    hasEverReceivedLocation = true
                    trySend(LocationState.Success(locationData))
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                if (isDisposed) return
                if (!availability.isLocationAvailable) {
                    // Only send error if we never had a location
                    if (!hasEverReceivedLocation) {
                        trySend(LocationState.Error("Location temporarily unavailable. Please ensure GPS is enabled and you're in an area with good signal."))
                    }
                    // If we have a location, silently ignore this error and keep showing the last location
                }
            }
        }.also { locationCallback = it }

        // Start with trying to get last known location for immediate response
        try {
            client.lastLocation.await()?.let { lastLocation ->
                if (isLocationRecent(lastLocation)) {
                    val locationData = lastLocation.toLocationData()
                    lastSuccessfulLocation = locationData
                    hasEverReceivedLocation = true
                    trySend(LocationState.Success(locationData))
                }
            }
        } catch (e: Exception) {
            // Ignore errors from last known location, continue with updates
        }

        client.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
            .addOnFailureListener { exception ->
                // Only send error if we never had a location
                if (!hasEverReceivedLocation) {
                    trySend(LocationState.Error("Failed to start location updates: ${exception.message}"))
                }
                // If we have a location, silently ignore this error
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
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
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

actual val currentPlatform: PlatformType get() = PlatformType.ANDROID

@Composable
actual fun getPlatformContext(): Any? = LocalContext.current