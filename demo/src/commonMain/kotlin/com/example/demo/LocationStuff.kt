package com.example.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eltonkola.ku.LocationConfig
import com.eltonkola.ku.LocationProvider
import com.eltonkola.ku.format


@Composable
fun Minimanl(){
    var label by remember { mutableStateOf("...") }

    LocationProvider(
        config = LocationConfig(singleRequest = true),
        onLocationReceived = { location ->
            label =  location.format()
        },
        onError = { errorMessage ->
            label = "Error: $errorMessage"
        }
    )

    Text(label)
}

@Composable
fun Custom(){

    LocationProvider(
        // Configure for continuous updates
        config = LocationConfig(
            singleRequest = false,
            highAccuracy = true,
            updateIntervalMs = 5000L
        ),
        // Initial view with a button to start the request
        onInitial = { onRequestLocation ->
            Button(onClick = { onRequestLocation() }) {
                Text("Find My Location")
            }
        },
        // Loading indicator
        onLoading = {
            CircularProgressIndicator()
        },
        // Custom permission denied dialog
        onPermissionDenied = { requestPermission ->
            PermissionDialog(onGrant = { requestPermission() })
        },
        // Custom error view with a retry button
        onError = { errorMessage, onRetry ->
            ErrorCard(errorMessage = errorMessage, onRetry = onRetry)

        },
        // Success view
        onLocationReceived = { location ->
            Text(text = "Latitude: ${location.latitude} - Longitude: ${location.longitude} - Accuracy: ${location.accuracy} - Altitude: ${location.altitude}")

        }
    )


}

@Composable
fun PermissionDialog(onGrant: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* Handle dismiss */ },
        title = { Text("Location Permission Required") },
        text = { Text("This app needs location access to show nearby places.") },
        confirmButton = {
            TextButton(onClick = { onGrant() }) {
                Text("Grant Permission")
            }
        }
    )
}

@Composable
fun ErrorCard(errorMessage: String, onRetry:() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location Error",
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Location Error: $errorMessage",
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }

    }
}
