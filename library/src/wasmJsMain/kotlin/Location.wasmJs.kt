package com.eltonkola.ku

import androidx.compose.runtime.Composable

actual class LocationClient {

    actual fun initialize(context: Any?) {
        // Wasm doesn't need context, but we'll handle it defensively
        if (context != null) {
            println("Warning: Context parameter is not used on Wasm")
        }
    }

    actual fun getLocation(): kotlinx.coroutines.flow.Flow<com.eltonkola.ku.LocationState> {
        TODO("Not yet implemented")
    }

    actual fun hasPermission(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun requestPermission() {
    }
    actual fun onDispose() {
    }
}
actual val currentPlatform: PlatformType
    get() = PlatformType.WASM

@Composable
actual fun getPlatformContext(): Any?  = null
