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
        var showMinimal by remember { mutableStateOf(false) }
        
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    showMinimal = !showMinimal
                }
            ) {
                Text(if(showMinimal) "Show Custom" else "Show Minimal")
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ){
                if(showMinimal){
                    Minimanl()
                }else{
                    Custom()
                }
            }


        }
    }
}

// Platform-specific expect/actual declarations
expect fun getPlatformName(): String
