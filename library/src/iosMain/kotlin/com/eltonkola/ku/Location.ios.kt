package com.eltonkola.ku

import kotlinx.coroutines.flow.Flow

actual class LocationClient {
    actual fun getLocation(): Flow<LocationState> {
        TODO("Not yet implemented")
    }

    actual fun hasPermission(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun requestPermission() {
    }
}