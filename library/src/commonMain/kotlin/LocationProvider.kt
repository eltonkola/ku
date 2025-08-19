package com.eltonkola.ku

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.PI

// Configuration class for cleaner API
data class LocationConfig(
    val autoRequest: Boolean = false,
    val singleRequest: Boolean = false,
    val highAccuracy: Boolean = true,
    val updateIntervalMs: Long = 10_000L,
    val minUpdateIntervalMs: Long = 5_000L,
    val maxUpdateDelayMs: Long = 15_000L,
    val timeoutMs: Long = 30_000L
)

// Main API - fully customizable UI
@Composable
fun LocationProvider(
    onLocationReceived: @Composable (Location) -> Unit,
    modifier: Modifier = Modifier,
    config: LocationConfig = LocationConfig(),
    onPermissionDenied: @Composable (() -> Unit) -> Unit = { requestPermission ->
        DefaultPermissionDeniedContent(onRequestPermission = requestPermission)
    },
    onLoading: @Composable () -> Unit = { DefaultLoadingContent() },
    onError: @Composable (String, () -> Unit) -> Unit = { error, retry ->
        DefaultErrorContent(error, retry)
    },
    onInitial: @Composable (() -> Unit) -> Unit = { onRequestLocation ->
        DefaultInitialContent(onRequestLocation = onRequestLocation)
    }
) {
    val locationClient = rememberLocationClient()
    var isLocationRequested by remember { mutableStateOf(config.autoRequest) }
    var retryTrigger by remember { mutableStateOf(0) }

    // Handle auto request
    LaunchedEffect(config.autoRequest, retryTrigger) {
        if (config.autoRequest && !isLocationRequested) {
            isLocationRequested = true
        }
    }

    Box(modifier = modifier) {
        if (isLocationRequested) {
            val locationState by locationClient.getLocation(config, retryTrigger)
                .collectAsState(initial = LocationState.Loading)

            when (val state = locationState) {
                is LocationState.Success -> onLocationReceived(state.location)
                LocationState.PermissionDenied -> onPermissionDenied {
                    locationClient.requestPermission()
                }
                is LocationState.Error -> onError(state.message) {
                    retryTrigger++
                }
                LocationState.Loading -> onLoading()
            }
        } else {
            onInitial { isLocationRequested = true }
        }
    }
}

// Convenience function for callback-style usage (no UI customization)
@Composable
fun LocationProvider(
    onLocationReceived: (Location) -> Unit,
    onError: (String) -> Unit = {},
    onPermissionDenied: () -> Unit = {},
    modifier: Modifier = Modifier,
    config: LocationConfig = LocationConfig()
) {
    LocationProvider(
        onLocationReceived = { location ->
            onLocationReceived(location)
            // Return empty composable since we're using callbacks
            Box {}
        },
        modifier = modifier,
        config = config,
        onPermissionDenied = { requestPermission ->
            onPermissionDenied()
            DefaultPermissionDeniedContent(onRequestPermission = requestPermission)
        },
        onLoading = { DefaultLoadingContent() },
        onError = { error, retry ->
            onError(error)
            DefaultErrorContent(error, retry)
        }
    )
}

@Composable
private fun rememberLocationClient(): LocationClient {
    val client = remember { LocationClient() }
    val context = getPlatformContext()

    DisposableEffect(client, context) {
        try {
            client.initialize(context)
        } catch (e: Exception) {
            // Handle initialization error
        }
        onDispose {
            client.onDispose()
        }
    }
    return client
}

// Default UI components (can be replaced by users)
@Composable
fun DefaultPermissionDeniedContent(onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier.padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Location Permission Required",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "We need location access to show your position",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
fun DefaultLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Getting your location...")
        }
    }
}

@Composable
fun DefaultErrorContent(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Location Error",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
fun DefaultInitialContent(onRequestLocation: () -> Unit) {
    Button(onClick = onRequestLocation) {
        Text("Get My Location")
    }
}

fun Location.format(precision: Int = 6): String {
    fun Double.roundToDecimals(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10.0 }
        return kotlin.math.round(this * multiplier) / multiplier
    }

    val lat = latitude.roundToDecimals(precision)
    val lon = longitude.roundToDecimals(precision)
    return "$lat, $lon"
}
fun Location.distanceTo(other: Location): Float {
    // Haversine formula for distance calculation
    val R = 6371000f // Earth's radius in meters

    // Convert degrees to radians
    fun toRad(degrees: Double) = degrees * (PI / 180)

    val dLat = toRad(other.latitude - latitude)
    val dLon = toRad(other.longitude - longitude)

    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(toRad(latitude)) *
            kotlin.math.cos(toRad(other.latitude)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)

    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return R * c.toFloat()
}

@Composable
fun useLocation(config: LocationConfig = LocationConfig()): LocationState {
    val locationClient = rememberLocationClient()
    var retryTrigger by remember { mutableStateOf(0) }

    val locationState by locationClient.getLocation(config, retryTrigger)
        .collectAsState(initial = LocationState.Loading)

    return locationState
}

