package com.eltonkola.ku

actual class LocationClient {
    actual fun getLocation(): kotlinx.coroutines.flow.Flow<com.eltonkola.ku.LocationState> {
        TODO("Not yet implemented")
    }

    actual fun hasPermission(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun requestPermission() {
    }
}