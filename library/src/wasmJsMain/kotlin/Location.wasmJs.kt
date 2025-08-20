package com.eltonkola.ku

import androidx.compose.runtime.Composable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// External declarations for browser APIs
external interface GeolocationPosition {
    val coords: GeolocationCoordinates
    val timestamp: Double
}

external interface GeolocationCoordinates {
    val latitude: Double
    val longitude: Double
    val accuracy: Double?
    val altitude: Double?
    val altitudeAccuracy: Double?
    val heading: Double?
    val speed: Double?
}

external interface GeolocationPositionError {
    val code: Short
    val message: String
}

external interface GeolocationOptions {
    val enableHighAccuracy: Boolean?
    val timeout: Int?
    val maximumAge: Int?
}

external interface Navigator {
    val geolocation: Geolocation
}

external interface Geolocation {
    fun getCurrentPosition(
        successCallback: (GeolocationPosition) -> Unit,
        errorCallback: ((GeolocationPositionError) -> Unit)? = definedExternally,
        options: GeolocationOptions? = definedExternally
    )

    fun watchPosition(
        successCallback: (GeolocationPosition) -> Unit,
        errorCallback: ((GeolocationPositionError) -> Unit)? = definedExternally,
        options: GeolocationOptions? = definedExternally
    ): Int

    fun clearWatch(watchId: Int)
}

external val navigator: Navigator

// Top-level JS functions (required for Kotlin/WASM)
private fun createGeolocationOptionsJs(
    enableHighAccuracy: Boolean,
    timeout: Int,
    maximumAge: Int
): GeolocationOptions = js("({ enableHighAccuracy: enableHighAccuracy, timeout: timeout, maximumAge: maximumAge })")

private fun checkGeolocationSupport(): Boolean = js("typeof navigator !== 'undefined' && 'geolocation' in navigator")

private fun checkSecureContext(): Boolean = js("typeof window !== 'undefined' && (window.isSecureContext || location.protocol === 'https:' || location.hostname === 'localhost')")

// Helper function to create GeolocationOptions
private fun createGeolocationOptions(
    enableHighAccuracy: Boolean = false,
    timeout: Int = 10000,
    maximumAge: Int = 60000
): GeolocationOptions {
    return createGeolocationOptionsJs(enableHighAccuracy, timeout, maximumAge)
}

actual class LocationClient actual constructor() {
    private var isDisposed = false
    private var watchId: Int? = null

    actual fun initialize(context: Any?) {
        if (!isGeolocationSupported()) {
            throw IllegalStateException("Geolocation is not supported in this browser")
        }
    }

    actual fun getLocation(config: LocationConfig, retryTrigger: Int): Flow<LocationState> =
        flow {
            emit(LocationState.Loading)

            if (isDisposed) {
                emit(LocationState.Error("LocationClient is disposed"))
                return@flow
            }

            if (!isGeolocationSupported()) {
                emit(LocationState.Error("Geolocation not supported in this browser"))
                return@flow
            }

            if (!isSecureContext()) {
                emit(LocationState.Error("Geolocation requires HTTPS. Please use a secure connection."))
                return@flow
            }

            try {
                if (config.singleRequest) {
                    getSingleLocation(config)?.let { location ->
                        emit(LocationState.Success(location))
                    } ?: emit(LocationState.Error("Unable to get current location"))
                } else {
                    getContinuousLocation(config).collect { state ->
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

    private suspend fun getSingleLocation(config: LocationConfig): Location? {
        return withTimeoutOrNull(config.timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                val options = createGeolocationOptions(
                    enableHighAccuracy = config.highAccuracy,
                    timeout = config.timeoutMs.toInt(),
                    maximumAge = 60000
                )

                navigator.geolocation.getCurrentPosition(
                    successCallback = { position ->
                        try {
                            continuation.resume(position.toLocation())
                        } catch (e: Exception) {
                            continuation.resumeWithException(e)
                        }
                    },
                    errorCallback = { error ->
                        continuation.resumeWithException(Exception(getErrorMessage(error)))
                    },
                    options = options
                )
            }
        }
    }

    private fun getContinuousLocation(config: LocationConfig): Flow<LocationState> = callbackFlow {
        val options = createGeolocationOptions(
            enableHighAccuracy = config.highAccuracy,
            timeout = config.timeoutMs.toInt(),
            maximumAge = 5000
        )

        watchId = navigator.geolocation.watchPosition(
            successCallback = { position ->
                if (!isDisposed) {
                    try {
                        trySend(LocationState.Success(position.toLocation()))
                    } catch (e: Exception) {
                        trySend(LocationState.Error("Failed to parse location: ${e.message}"))
                    }
                }
            },
            errorCallback = { error ->
                if (!isDisposed) {
                    trySend(LocationState.Error(getErrorMessage(error)))
                }
            },
            options = options
        )

        awaitClose { cleanup() }
    }

    actual fun hasPermission(): Boolean = isGeolocationSupported()

    actual fun requestPermission() {
        if (isGeolocationSupported()) {
            navigator.geolocation.getCurrentPosition(
                successCallback = { /* Permission granted */ },
                errorCallback = { /* Permission denied or error */ },
                options = createGeolocationOptions(
                    enableHighAccuracy = false,
                    timeout = 1000,
                    maximumAge = 86400000
                )
            )
        }
    }

    private fun cleanup() {
        watchId?.let { navigator.geolocation.clearWatch(it) }
        watchId = null
    }

    actual fun onDispose() {
        isDisposed = true
        cleanup()
    }

    private fun isGeolocationSupported(): Boolean {
        return try {
            checkGeolocationSupport()
        } catch (e: Exception) {
            false
        }
    }

    private fun getErrorMessage(error: GeolocationPositionError): String {
        return when (error.code.toInt()) {
            1 -> "Location access denied by user"
            2 -> "Location information unavailable"
            3 -> "Location request timeout"
            else -> "Unknown location error: ${error.message}"
        }
    }
}

// Extension function to convert browser Position to our Location
private fun GeolocationPosition.toLocation(): Location {
    return Location(
        latitude = coords.latitude,
        longitude = coords.longitude,
        accuracy = coords.accuracy?.toFloat(),
        altitude = coords.altitude,
        speed = coords.speed?.toFloat(),
        bearing = coords.heading?.toFloat(),
        timestamp = timestamp.toLong(),
        provider = "Browser Geolocation API"
    )
}

actual val currentPlatform: PlatformType
    get() = PlatformType.WASM

@Composable
actual fun getPlatformContext(): Any? = null

fun isSecureContext(): Boolean {
    return try {
        checkSecureContext()
    } catch (e: Exception) {
        false
    }
}