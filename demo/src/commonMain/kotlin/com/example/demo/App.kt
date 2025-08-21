package com.example.demo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun App() {
    MaterialTheme {
        var locationText by remember { mutableStateOf("Press button to get location") }
        
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("KMM Location Demo", style = MaterialTheme.typography.headlineMedium)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    // This will be implemented in platform-specific code
                    locationText = "Getting location..."
                }
            ) {
                Text("Get My Location")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(locationText, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// Platform-specific expect/actual declarations
expect fun getPlatformName(): String
