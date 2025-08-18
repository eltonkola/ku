package com.eltonkola.ku

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

data class Location(
    val latitude: Double,
    val longitude: Double,
    // Optional fields (nullable for platforms that don't support them)
    val accuracy: Float? = null,        // Horizontal accuracy in meters
    val altitude: Double? = null,       // Height above sea level in meters
    val speed: Float? = null,           // Speed in m/s
    val bearing: Float? = null,         // Degrees from true north (0-360)
    val timestamp: Long? = null,        // UTC epoch milliseconds
    val provider: String? = null
)

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
