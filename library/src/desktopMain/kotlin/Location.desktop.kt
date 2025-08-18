package com.eltonkola.ku

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

actual class LocationClient {

    actual fun initialize(context: Any?) {
        // Desktop doesn't need context, but we'll handle it defensively
        if (context != null) {
            println("Warning: Context parameter is not used on Desktop")
        }
    }

    actual fun getLocation(): Flow<LocationState> {
        return emptyFlow()
    }

    actual fun hasPermission(): Boolean {
        return true
    }

    actual fun requestPermission() {
    }

    actual fun onDispose() {
    }
}

actual val currentPlatform: PlatformType
    get() = PlatformType.DESKTOP

@Composable
actual fun getPlatformContext(): Any?  = null