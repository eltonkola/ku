package com.eltonkola.ku

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class Location @OptIn(ExperimentalTime::class) constructor(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val altitude: Double? = null,
    val speed: Float? = null,
    val bearing: Float? = null,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val provider: String? = null
) {
    val isAccurate: Boolean get() = accuracy?.let { it <= 50f } ?: false
    val hasMovement: Boolean get() = speed?.let { it > 0.5f } ?: false
}

sealed class LocationState {
    object Loading : LocationState()
    data class Success(val location: Location) : LocationState()
    data class Error(val message: String) : LocationState()
    object PermissionDenied : LocationState()
}

expect class LocationClient() {
    fun initialize(context: Any?)
    fun getLocation(config: LocationConfig, retryTrigger: Int): Flow<LocationState>
    fun hasPermission(): Boolean
    fun requestPermission()
    fun onDispose()
}

enum class PlatformType {
    ANDROID, IOS, DESKTOP, WASM
}

expect val currentPlatform: PlatformType

@Composable
expect fun getPlatformContext(): Any?


