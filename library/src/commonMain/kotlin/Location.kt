package com.eltonkola.ku

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

data class Location(val latitude: Double, val longitude: Double)

sealed class LocationState {
    object Loading : LocationState()
    data class Success(val location: Location) : LocationState()
    data class Error(val message: String) : LocationState()
    object PermissionDenied : LocationState()
}

expect class LocationClient() {
    fun initialize(context: Any?)
    fun getLocation(): Flow<LocationState>
    fun hasPermission(): Boolean
    fun requestPermission()
    fun onDispose()
}


enum class PlatformType {
    ANDROID,
    IOS,
    DESKTOP,
    WASM
}

expect val currentPlatform: PlatformType

@Composable
expect fun getPlatformContext(): Any?
