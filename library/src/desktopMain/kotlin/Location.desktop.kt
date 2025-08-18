package com.eltonkola.ku

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.net.HttpURLConnection
import java.net.URL

actual class LocationClient {

    actual fun initialize(context: Any?) {
        // Desktop doesn't need context, but we'll handle it defensively
        if (context != null) {
            println("Warning: Context parameter is not used on Desktop")
        }
    }

    actual fun getLocation(): Flow<LocationState> = flow {
        emit(LocationState.Loading)
        try {
            val json = fetchJsonFromUrl("https://ipapi.co/json/")
            val location = parseIpApiResponse(json)
            emit(LocationState.Success(location))
        } catch (e: Exception) {
            emit(LocationState.Error(e.message ?: "Network error"))
        }
    }

    private fun fetchJsonFromUrl(urlString: String): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun parseIpApiResponse(json: String): Location {
        // Manual JSON parsing (no kotlinx.serialization)
        val lat = json.extractDouble("\"latitude\":")
            ?: error("Missing latitude")
        val lon = json.extractDouble("\"longitude\":")
            ?: error("Missing longitude")

        return Location(
            latitude = lat,
            longitude = lon,
            accuracy = 5000f, // Default IP API accuracy (~5km)
            provider = "ipapi.co"
        )
    }

    private fun String.extractDouble(key: String): Double? {
        val startIdx = indexOf(key) + key.length
        val endIdx = indexOfAny(charArrayOf(',', '}'), startIdx)
        return substring(startIdx, endIdx).toDoubleOrNull()
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