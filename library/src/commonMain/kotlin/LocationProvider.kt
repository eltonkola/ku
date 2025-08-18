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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun LocationProvider(
    permissionDeniedContent: @Composable (() -> Unit) -> Unit = { requestPermission ->
        DefaultPermissionDeniedContent(onRequestPermission = requestPermission)
    },
    loadingContent: @Composable () -> Unit = { DefaultLoadingContent() },
    errorContent: @Composable (String) -> Unit = { DefaultErrorContent(it) },
    content: @Composable (Location) -> Unit,
    requestButton: @Composable (() -> Unit) -> Unit = { onClick ->
        DefaultRequestButton(onClick = onClick)
    },
    requestOnStart: Boolean = false
) {
    //println("LocationProvider init : $currentPlatform")
    val locationClient = rememberLocationClient()
    var shouldRequestLocation by remember(requestOnStart) { mutableStateOf(requestOnStart) }
    val locationState by locationClient.getLocation().collectAsState(initial = LocationState.Loading)

    if (shouldRequestLocation) {
        when (locationState) {
            is LocationState.Success -> {
                content((locationState as LocationState.Success).location)
            }
            LocationState.PermissionDenied -> {
                permissionDeniedContent { locationClient.requestPermission() }
            }
            is LocationState.Error -> {
                errorContent((locationState as LocationState.Error).message)
            }
            LocationState.Loading -> {
                loadingContent()
            }
        }
    }else{
        requestButton { shouldRequestLocation = true }
    }

}

@Composable
private fun DefaultRequestButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
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
private fun DefaultPermissionDeniedContent(onRequestPermission: () -> Unit) {
    Column {
        Text("We need location permission to show your position")
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}

@Composable
private fun DefaultLoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun DefaultErrorContent(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Error: $message", color = MaterialTheme.colorScheme.error)
    }
}