package com.eltonkola.ku

import androidx.compose.runtime.Composable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow

@JsFun("() => navigator.geolocation.getCurrentPosition !== undefined")
private external fun hasGeolocation(): Boolean

@JsFun("""
(successCallback, errorCallback) => {
    const successWrapper = (pos) => successCallback(
        pos.coords.latitude,
        pos.coords.longitude,
        pos.coords.accuracy,
        pos.coords.speed ?? null,
        pos.timestamp
    );
    const errorWrapper = (err) => errorCallback(err.code, err.message);
    return navigator.geolocation.watchPosition(
        successWrapper,
        errorWrapper,
        { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
    );
}
""")
private external fun watchPosition(
    onSuccess: (lat: Double, lng: Double, accuracy: Double, speed: Double?, timestamp: Double) -> Unit,
    onError: (code: Int, message: String) -> Unit
): Int

@JsFun("(id) => navigator.geolocation.clearWatch(id)")
private external fun clearWatch(id: Int)

actual class LocationClient actual constructor() {
    actual fun getLocation(): Flow<LocationState> = callbackFlow {
        if (!hasGeolocation()) {
            trySend(LocationState.Error("Geolocation API not available"))
            close()
            return@callbackFlow
        }

        trySend(LocationState.Loading)

        val watchId = watchPosition(
            onSuccess = { lat, lng, accuracy, speed, timestamp ->
                trySend(
                    LocationState.Success(
                        Location(
                            latitude = lat,
                            longitude = lng,
                            accuracy = accuracy.toFloat(),
                            speed = speed?.toFloat(),
                            timestamp = timestamp.toLong(),
                            provider = "web"
                        )
                    )
                )
            },
            onError = { code, message ->
                trySend(
                    when (code) {
                        1 -> LocationState.PermissionDenied
                        2 -> LocationState.Error("Position unavailable: $message")
                        3 -> LocationState.Error("Timeout: $message")
                        else -> LocationState.Error("Unknown error: $message")
                    }
                )
            }
        )

        awaitClose {
            clearWatch(watchId)
        }
    }

    actual fun initialize(context: Any?) = Unit
    actual fun hasPermission(): Boolean = true
    actual fun requestPermission() = Unit
    actual fun onDispose() = Unit
}

actual val currentPlatform: PlatformType
    get() = PlatformType.WASM

@Composable
actual fun getPlatformContext(): Any? = null