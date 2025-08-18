package com.eltonkola.ku

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
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

@Composable
fun LocationProvider(
    onPermissionDenied: @Composable (() -> Unit) -> Unit = { requestPermission ->
        DefaultPermissionDeniedUi(onRequestPermission = requestPermission)
    },
    onLoading: @Composable () -> Unit = { DefaultLoadingUi() },
    onError: @Composable (String) -> Unit = { DefaultErrorUi(it) },
    onLocationReceived: @Composable (Location) -> Unit,
    initialContent: @Composable (() -> Unit) -> Unit = { onRequestLocation ->
        DefaultInitialUi(onRequestLocation = onRequestLocation)
    },
    autoRequest: Boolean = false
) {
    val locationClient = rememberLocationClient()
    var isLocationRequested by remember { mutableStateOf(false) }

    // Handle auto request on start
    LaunchedEffect(autoRequest) {
        if (autoRequest) {
            isLocationRequested = true
        }
    }

    if (isLocationRequested) {
        val locationState by locationClient.getLocation().collectAsState(initial = LocationState.Loading)

        when (val state = locationState) {
            is LocationState.Success -> onLocationReceived(state.location)
            LocationState.PermissionDenied -> onPermissionDenied { locationClient.requestPermission() }
            is LocationState.Error -> onError(state.message)
            LocationState.Loading -> onLoading()
        }
    } else {
        initialContent { isLocationRequested = true }
    }
}

@Composable
private fun DefaultInitialUi(onRequestLocation: () -> Unit) {
    Button(onClick = onRequestLocation) {
        Text("Get My Location")
    }
}

@Composable
private fun rememberLocationClient(): LocationClient {
    val client = remember { LocationClient() }
    val context = getPlatformContext()

    DisposableEffect(client, context) {
        client.initialize(context)
        onDispose {
            client.onDispose()
        }
    }
    return client
}

// Default UI components
@Composable
private fun DefaultPermissionDeniedUi(onRequestPermission: () -> Unit) {
    Column {
        Text("We need location permission to show your position")
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}

@Composable
private fun DefaultLoadingUi() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun DefaultErrorUi(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Error: $message", color = MaterialTheme.colorScheme.error)
    }
}