package com.eltonkola.ku

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.*
import platform.darwin.NSObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.timeIntervalSince1970

actual class LocationClient actual constructor() {
    private val manager = CLLocationManager().apply {
        desiredAccuracy = kCLLocationAccuracyBest
        distanceFilter = 10.0 // Minimum distance (meters) for updates
    }

    private var delegate: LocationDelegate? = null

    actual fun initialize(context: Any?) {
        // No context needed for iOS
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun getLocation(): Flow<LocationState> = callbackFlow {
        val delegate = LocationDelegate { location ->
            trySend(
                LocationState.Success(
                    Location(
                        latitude = location.coordinate.latitude,
                        longitude = location.coordinate.longitude,
                        accuracy = location.horizontalAccuracy.toFloat(),
                        altitude = location.altitude,
                        speed = location.speed.toFloat(),
                        bearing = location.course.toFloat().takeIf { it >= 0 },
                        timestamp = location.timestamp.toEpochMillis()
                    )
                )
            )
        }

        this@LocationClient.delegate = delegate
        manager.delegate = delegate

        when (CLLocationManager.authorizationStatus()) {
            kCLAuthorizationStatusNotDetermined -> manager.requestWhenInUseAuthorization()
            kCLAuthorizationStatusDenied -> trySend(LocationState.PermissionDenied)
            else -> Unit
        }

        manager.startUpdatingLocation()

        awaitClose {
            manager.stopUpdatingLocation()
            manager.delegate = null
            this@LocationClient.delegate = null
        }
    }

    actual fun hasPermission(): Boolean {
        return when (CLLocationManager.authorizationStatus()) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> true
            else -> false
        }
    }

    actual fun requestPermission() {
        manager.requestWhenInUseAuthorization()
    }

    actual fun onDispose() {
        manager.stopUpdatingLocation()
        manager.delegate = null
        delegate = null
    }

    private class LocationDelegate(
        private val onLocationUpdate: (CLLocation) -> Unit
    ) : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(
            manager: CLLocationManager,
            didUpdateLocations: List<*>
        ) {
            (didUpdateLocations.firstOrNull() as? CLLocation)?.let(onLocationUpdate)
        }

        override fun locationManager(
            manager: CLLocationManager,
            didFailWithError: NSError
        ) {
            // Handle errors if needed
        }
    }
}

private fun NSDate.toEpochMillis(): Long =
    (timeIntervalSince1970 * 1000).toLong()

actual val currentPlatform: PlatformType
    get() = PlatformType.IOS

@Composable
actual fun getPlatformContext(): Any?  = null