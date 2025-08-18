package com.eltonkola.ku

import kotlinx.coroutines.flow.Flow

data class Location(val latitude: Double, val longitude: Double)

sealed class LocationState {
    object Loading : LocationState()
    data class Success(val location: Location) : LocationState()
    data class Error(val message: String) : LocationState()
    object PermissionDenied : LocationState()
}

expect class LocationClient {
    fun getLocation(): Flow<LocationState>
    fun hasPermission(): Boolean
    fun requestPermission()
}
