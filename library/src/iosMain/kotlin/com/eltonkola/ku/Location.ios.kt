// iosMain/kotlin/com/eltonkola/ku/LocationClient.kt
package com.eltonkola.ku

import androidx.compose.runtime.Composable
import kotlinx.cinterop.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.*
import platform.Foundation.*
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class LocationClient actual constructor() {
    private var locationManager: CLLocationManager? = null
    private var currentDelegate: NSObject? = null
    private var isDisposed = false

    actual fun initialize(context: Any?) {
        // iOS doesn't need context, but we can accept it for consistency
        locationManager = CLLocationManager()
    }

    actual fun getLocation(config: LocationConfig, retryTrigger: Int): Flow<LocationState> =
        flow {
            emit(LocationState.Loading)

            if (isDisposed) {
                emit(LocationState.Error("LocationClient is disposed"))
                return@flow
            }

            val manager = locationManager
                ?: throw IllegalStateException("LocationClient not initialized")

            // Check authorization status
            when (manager.authorizationStatus) {
                kCLAuthorizationStatusDenied, kCLAuthorizationStatusRestricted -> {
                    emit(LocationState.PermissionDenied)
                    return@flow
                }
                kCLAuthorizationStatusNotDetermined -> {
                    emit(LocationState.PermissionDenied)
                    return@flow
                }
            }

            // Check if location services are enabled
            if (!CLLocationManager.locationServicesEnabled()) {
                emit(LocationState.Error("Location services are disabled"))
                return@flow
            }

            try {
                if (config.singleRequest) {
                    getSingleLocation(manager, config)?.let { location ->
                        emit(LocationState.Success(location))
                    } ?: emit(LocationState.Error("Unable to get current location"))
                } else {
                    getContinuousLocation(manager, config).collect { state ->
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

    private suspend fun getSingleLocation(
        manager: CLLocationManager,
        config: LocationConfig
    ): Location? {
        return withTimeoutOrNull(config.timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                        val locations = didUpdateLocations.mapNotNull { it as? CLLocation }
                        val latestLocation = locations.lastOrNull()

                        manager.stopUpdatingLocation()
                        manager.delegate = null
                        currentDelegate = null

                        if (latestLocation != null) {
                            continuation.resume(latestLocation.toCoreLocation())
                        } else {
                            continuation.resume(null)
                        }
                    }

                    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                        manager.stopUpdatingLocation()
                        manager.delegate = null
                        currentDelegate = null
                        continuation.resumeWithException(Exception("Location error: ${didFailWithError.localizedDescription}"))
                    }

                    override fun locationManager(manager: CLLocationManager, didChangeAuthorizationStatus: CLAuthorizationStatus) {
                        when (didChangeAuthorizationStatus) {
                            kCLAuthorizationStatusDenied,
                            kCLAuthorizationStatusRestricted -> {
                                manager.delegate = null
                                currentDelegate = null
                                continuation.resumeWithException(Exception("Location permission denied"))
                            }
                            kCLAuthorizationStatusAuthorizedWhenInUse,
                            kCLAuthorizationStatusAuthorizedAlways -> {
                                manager.requestLocation()
                            }
                        }
                    }
                }

                currentDelegate = delegate
                manager.delegate = delegate

                // Configure accuracy
                manager.desiredAccuracy = if (config.highAccuracy) {
                    kCLLocationAccuracyBest
                } else {
                    kCLLocationAccuracyHundredMeters
                }

                manager.requestLocation()

                continuation.invokeOnCancellation {
                    manager.stopUpdatingLocation()
                    manager.delegate = null
                    currentDelegate = null
                }
            }
        }
    }

    private fun getContinuousLocation(
        manager: CLLocationManager,
        config: LocationConfig
    ): Flow<LocationState> = callbackFlow {
        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                if (isDisposed) return

                val locations = didUpdateLocations.mapNotNull { it as? CLLocation }
                locations.forEach { clLocation ->
                    trySend(LocationState.Success(clLocation.toCoreLocation()))
                }
            }

            override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                if (isDisposed) return
                trySend(LocationState.Error("Location error: ${didFailWithError.localizedDescription}"))
            }

            override fun locationManager(manager: CLLocationManager, didChangeAuthorizationStatus: CLAuthorizationStatus) {
                if (isDisposed) return

                when (didChangeAuthorizationStatus) {
                    kCLAuthorizationStatusDenied,
                    kCLAuthorizationStatusRestricted -> {
                        trySend(LocationState.PermissionDenied)
                    }
                    kCLAuthorizationStatusNotDetermined -> {
                        manager.requestWhenInUseAuthorization()
                    }
                    kCLAuthorizationStatusAuthorizedWhenInUse,
                    kCLAuthorizationStatusAuthorizedAlways -> {
                        manager.startUpdatingLocation()
                    }
                }
            }
        }

        currentDelegate = delegate
        manager.delegate = delegate

        // Configure location manager
        manager.desiredAccuracy = if (config.highAccuracy) {
            kCLLocationAccuracyBest
        } else {
            kCLLocationAccuracyHundredMeters
        }

        // Set distance filter (minimum distance for updates)
        manager.distanceFilter = 10.0 // 10 meters

        manager.startUpdatingLocation()

        awaitClose {
            cleanup()
        }
    }

    actual fun hasPermission(): Boolean {
        val manager = locationManager ?: return false
        return when (manager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> true
            else -> false
        }
    }

    actual fun requestPermission() {
        locationManager?.requestWhenInUseAuthorization()
    }

    private fun cleanup() {
        locationManager?.stopUpdatingLocation()
        locationManager?.delegate = null
        currentDelegate = null
    }

    actual fun onDispose() {
        isDisposed = true
        cleanup()
        locationManager = null
    }
}

// Extension function to convert CLLocation to our Location data class
@OptIn(ExperimentalForeignApi::class)
private fun CLLocation.toCoreLocation(): Location {
    return this.coordinate.useContents {
        Location(
            latitude = this.latitude,
            longitude = this.longitude,
            accuracy = horizontalAccuracy.toFloat().takeIf { it >= 0 },
            altitude = altitude.takeIf { verticalAccuracy >= 0 },
            speed = speed.toFloat().takeIf { it >= 0 },
            bearing = course.toFloat().takeIf { it >= 0 },
            timestamp = (timestamp.timeIntervalSince1970 * 1000).toLong(),
            provider = "CoreLocation"
        )
    }
}

actual val currentPlatform: PlatformType
    get() = PlatformType.IOS

@Composable
actual fun getPlatformContext(): Any? = null // iOS doesn't need context