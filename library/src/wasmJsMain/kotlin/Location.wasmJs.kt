package com.eltonkola.ku

import androidx.compose.runtime.Composable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


@JsFun("""
() => {
    const obj = {};
    obj.enableHighAccuracy = true;
    obj.timeout = 10000;
    obj.maximumAge = 0;
    return obj;
}
""")
private external fun createGeolocationOptions(): Any

@JsFun("""
(success, error, options) => {
    return navigator.geolocation.watchPosition(
        pos => success(pos),
        err => error(err),
        options
    );
}
""")
private external fun startWatchPosition(
    success: (Any) -> Unit,
    error: (Any) -> Unit,
    options: Any
): Int

@JsFun("(id) => navigator.geolocation.clearWatch(id)")
private external fun stopWatchPosition(id: Int)

@JsFun("() => 'geolocation' in navigator")
private external fun hasGeolocation(): Boolean

@JsFun("(obj, path) => path.split('.').reduce((o,p) => o?.[p], obj)")
private external fun getDouble(obj: Any, path: String): Double

@JsFun("(obj, path) => path.split('.').reduce((o,p) => o?.[p], obj)")
private external fun getFloat(obj: Any, path: String): Float

@JsFun("(obj, path) => path.split('.').reduce((o,p) => o?.[p], obj)")
private external fun getNullableFloat(obj: Any, path: String): Float?

@JsFun("(obj, path) => path.split('.').reduce((o,p) => o?.[p], obj)")
private external fun getLong(obj: Any, path: String): Long

@JsFun("(obj, path) => path.split('.').reduce((o,p) => o?.[p], obj)")
private external fun getInt(obj: Any, path: String): Int

actual class LocationClient actual constructor() {
    actual fun getLocation(): Flow<LocationState> = callbackFlow {
        if (!hasGeolocation()) {
            trySend(LocationState.Error("Geolocation API not available"))
            close()
            return@callbackFlow
        }

        trySend(LocationState.Loading)

        val options = createGeolocationOptions()

        val success = { pos: Any ->
            trySend(
                LocationState.Success(
                    Location(
                        latitude = getDouble(pos, "coords.latitude"),
                        longitude = getDouble(pos, "coords.longitude"),
                        accuracy = getFloat(pos, "coords.accuracy"),
                        speed = getNullableFloat(pos, "coords.speed"),
                        timestamp = getLong(pos, "timestamp"),
                        provider = "web"
                    )
                )
            )
        }

        val error = { err: Any ->
            trySend(
                when (getInt(err, "code")) {
                    1 -> LocationState.PermissionDenied
                    2 -> LocationState.Error("Position unavailable")
                    3 -> LocationState.Error("Timeout")
                    else -> LocationState.Error("Unknown error")
                }
            )
        }

        val watchId = startWatchPosition(
            success = { pos -> success(pos) },
            error = { err -> error(err) },
            options = options
        )

        awaitClose {
            stopWatchPosition(watchId)
        }
    }


    // Stub implementations
    actual fun initialize(context: Any?) = Unit
    actual fun hasPermission(): Boolean = false
    actual fun requestPermission() = Unit
    actual fun onDispose() = Unit
}


actual val currentPlatform: PlatformType
    get() = PlatformType.WASM

@Composable
actual fun getPlatformContext(): Any?  = null
