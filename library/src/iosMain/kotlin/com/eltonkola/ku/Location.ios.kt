package com.eltonkola.ku

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

actual class LocationClient {

    actual fun initialize(context: Any?) {
        // iOS doesn't need context, but we'll handle it defensively
        if (context != null) {
            println("Warning: Context parameter is not used on iOS")
        }
    }

    actual fun getLocation(): Flow<LocationState> {
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
    get() = PlatformType.IOS

@Composable
actual fun getPlatformContext(): Any?  = null