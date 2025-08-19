package com.eltonkola.ku

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.URL

actual class LocationClient actual constructor() {
    private var isDisposed = false

    actual fun initialize(context: Any?) {
        // Desktop doesn't need context, but we'll handle it defensively
        if (context != null) {
            println("Warning: Context parameter is not used on Desktop")
        }
    }

    actual fun getLocation(config: LocationConfig, retryTrigger: Int): Flow<LocationState> =
        flow {
            emit(LocationState.Loading)

            if (isDisposed) {
                emit(LocationState.Error("LocationClient is disposed"))
                return@flow
            }

            try {
                val location = if (config.singleRequest) {
                    getSingleLocation(config)
                } else {
                    // For desktop, continuous updates don't make sense with IP geolocation
                    // So we'll just get a single location regardless
                    getSingleLocation(config)
                }

                location?.let {
                    emit(LocationState.Success(it))
                } ?: emit(LocationState.Error("Unable to get location from IP geolocation"))

            } catch (e: Exception) {
                emit(LocationState.Error("Location request failed: ${e.message}"))
            }
        }
            .catch { e ->
                emit(LocationState.Error("Location flow error: ${e.message}"))
            }
            .distinctUntilChanged()

    private suspend fun getSingleLocation(config: LocationConfig): Location? {
        return withTimeoutOrNull(config.timeoutMs) {
            try {
                // Try multiple IP geolocation services for better reliability
                val services = listOf(
                    IpApiService(),
                    IpInfoService(),
                    FreeGeoIpService()
                )

                for (service in services) {
                    try {
                        return@withTimeoutOrNull service.getLocation()
                    } catch (e: Exception) {
                        // Try next service if current one fails
                        continue
                    }
                }
                null
            } catch (e: Exception) {
                null
            }
        }
    }

    actual fun hasPermission(): Boolean {
        // Desktop doesn't need location permissions for IP geolocation
        return true
    }

    actual fun requestPermission() {
        // No permissions needed on desktop
    }

    actual fun onDispose() {
        isDisposed = true
    }
}

// Abstract service interface for different IP geolocation providers
private abstract class IpGeolocationService {
    abstract suspend fun getLocation(): Location

    protected fun fetchJsonFromUrl(urlString: String): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("User-Agent", "LocationLib/1.0")
        }
        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    protected fun String.extractDouble(key: String): Double? {
        val startIdx = indexOf(key)
        if (startIdx == -1) return null

        val valueStart = startIdx + key.length
        val valueEnd = indexOfAny(charArrayOf(',', '}', '\n', '\r'), valueStart)
            .takeIf { it != -1 } ?: length

        return substring(valueStart, valueEnd)
            .trim()
            .removeSurrounding("\"")
            .toDoubleOrNull()
    }

    protected fun String.extractString(key: String): String? {
        val startIdx = indexOf(key)
        if (startIdx == -1) return null

        val valueStart = startIdx + key.length
        val firstQuote = indexOf("\"", valueStart)
        if (firstQuote == -1) return null

        val secondQuote = indexOf("\"", firstQuote + 1)
        if (secondQuote == -1) return null

        return substring(firstQuote + 1, secondQuote)
    }
}

// Primary service - ipapi.co (your original choice)
private class IpApiService : IpGeolocationService() {
    override suspend fun getLocation(): Location {
        val json = fetchJsonFromUrl("https://ipapi.co/json/")
        val lat = json.extractDouble("\"latitude\":")
            ?: throw Exception("Missing latitude in response")
        val lon = json.extractDouble("\"longitude\":")
            ?: throw Exception("Missing longitude in response")
        val city = json.extractString("\"city\":")
        val country = json.extractString("\"country_name\":")

        return Location(
            latitude = lat,
            longitude = lon,
            accuracy = 5000f, // IP geolocation accuracy (~5km)
            provider = "ipapi.co${city?.let { " ($it${country?.let { ", $it" } ?: ""})" } ?: ""}"
        )
    }
}

// Backup service 1 - ipinfo.io
private class IpInfoService : IpGeolocationService() {
    override suspend fun getLocation(): Location {
        val json = fetchJsonFromUrl("https://ipinfo.io/json")
        val loc = json.extractString("\"loc\":")
            ?: throw Exception("Missing location in response")

        val (latStr, lonStr) = loc.split(",")
        val lat = latStr.toDoubleOrNull() ?: throw Exception("Invalid latitude")
        val lon = lonStr.toDoubleOrNull() ?: throw Exception("Invalid longitude")
        val city = json.extractString("\"city\":")
        val region = json.extractString("\"region\":")

        return Location(
            latitude = lat,
            longitude = lon,
            accuracy = 8000f, // Slightly less accurate
            provider = "ipinfo.io${city?.let { " ($it${region?.let { ", $it" } ?: ""})" } ?: ""}"
        )
    }
}

// Backup service 2 - freegeoip.app
private class FreeGeoIpService : IpGeolocationService() {
    override suspend fun getLocation(): Location {
        val json = fetchJsonFromUrl("https://freegeoip.app/json/")
        val lat = json.extractDouble("\"latitude\":")
            ?: throw Exception("Missing latitude in response")
        val lon = json.extractDouble("\"longitude\":")
            ?: throw Exception("Missing longitude in response")
        val city = json.extractString("\"city\":")
        val country = json.extractString("\"country_name\":")

        return Location(
            latitude = lat,
            longitude = lon,
            accuracy = 10000f, // Least accurate backup
            provider = "freegeoip.app${city?.let { " ($it${country?.let { ", $it" } ?: ""})" } ?: ""}"
        )
    }
}

actual val currentPlatform: PlatformType
    get() = PlatformType.DESKTOP

@Composable
actual fun getPlatformContext(): Any? = null