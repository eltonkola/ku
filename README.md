# KU 📍

[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-blue.svg)](https://kotlinlang.org/docs/multiplatform.html)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-green.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.eltonkola/ku)](https://search.maven.org/artifact/io.github.eltonkola/ku)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A beautiful, declarative Kotlin Multiplatform library for effortless location handling in Compose applications. KU (short for "where are yoU?") simplifies location permissions and fetching with a clean, composable API.

## ✨ Features

- 🎯 **Simple & Declarative** - One composable handles everything
- 🔒 **Permission Management** - Automatic permission handling
- 🎨 **Fully Customizable** - Override any UI component
- 📱 **Kotlin Multiplatform** - Works on Android, iOS, and more
- ⚡ **Compose-First** - Built specifically for Jetpack/Compose Multiplatform
- 🛡️ **Type-Safe** - Leverages Kotlin's type system for reliability

## 🚀 Quick Start

### Installation

Add KU to your `commonMain` dependencies:

```kotlin
commonMain {
    dependencies {
        implementation("io.github.eltonkola:ku:0.0.2")
    }
}
```

## 📖 Usage Guide

### Auto-Request Location

Start requesting location immediately when the composable loads:

```kotlin
LocationProvider(
    autoRequest = true,
    onLocationReceived = { location ->
        ShowLocationOnMap(location)
    }
)
```
That's it! KU handles permissions, loading states, and errors automatically.

### Custom UI Components

Override any part of the UI to match your design:

```kotlin
LocationProvider(
    initialContent = { onRequestLocation ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onRequestLocation() }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spaceBetween
            ) {
                Text("Find my location")
                Icon(Icons.Default.LocationOn, contentDescription = null)
            }
        }
    },
    onLoading = {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Finding your location...", style = MaterialTheme.typography.bodyMedium)
        }
    },
    onPermissionDenied = { requestPermission ->
        AlertDialog(
            onDismissRequest = { /* Handle dismiss */ },
            title = { Text("Location Permission Required") },
            text = { Text("This app needs location access to show nearby places.") },
            confirmButton = {
                TextButton(onClick = { requestPermission() }) {
                    Text("Grant Permission")
                }
            }
        )
    },
    onError = { errorMessage ->
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(
                text = "Location Error: $errorMessage",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    },
    onLocationReceived = { location ->
        LazyColumn {
            item {
                LocationCard(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy
                )
            }
        }
    }
)
```

### Error Handling

Handle location errors gracefully:

```kotlin
LocationProvider(
    onError = { errorMessage ->
        when {
            errorMessage.contains("GPS") -> {
                Column {
                    Text("GPS is disabled")
                    Button(
                        onClick = { /* Open location settings */ }
                    ) {
                        Text("Enable GPS")
                    }
                }
            }
            errorMessage.contains("network") -> {
                Text("Check your internet connection")
            }
            else -> {
                Text("Location unavailable: $errorMessage")
            }
        }
    },
    onLocationReceived = { location ->
        MapView(location)
    }
)
```

## 🎨 API Reference

### LocationProvider Parameters

| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| `onLocationReceived` | `@Composable (Location) -> Unit` | Content displayed when location is successfully obtained | **Required** |
| `initialContent` | `@Composable (() -> Unit) -> Unit` | Content displayed before location request | Default button |
| `onLoading` | `@Composable () -> Unit` | Content displayed while fetching location | Loading spinner |
| `onPermissionDenied` | `@Composable (() -> Unit) -> Unit` | Content displayed when permission is denied | Permission request UI |
| `onError` | `@Composable (String) -> Unit` | Content displayed when an error occurs | Error message |
| `autoRequest` | `Boolean` | Whether to request location automatically | `false` |

### Location Data

```kotlin
data class Location(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double?,
    val bearing: Float?,
    val speed: Float?,
    val timestamp: Long
)
```

## 📱 Platform Setup

### Android

Add location permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

### iOS

Add location usage description to your `Info.plist`:

```xml
<key>NSLocationWhenInUseUsageDescription</key>
<string>This app needs location access to show your current position.</string>
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

## 📄 License

```
Copyright 2024 Elton Kola

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

<div align="center">
  <sub>Built with ❤️ by <a href="https://github.com/eltonkola">Elton Kola</a></sub>
</div>