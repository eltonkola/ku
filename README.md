# KU 📍

[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-blue.svg)](https://kotlinlang.org/docs/multiplatform.html)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-green.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.eltonkola/ku)](https://search.maven.org/artifact/io.github.eltonkola/ku)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A beautiful, declarative Kotlin Multiplatform library for effortless location handling in Compose applications. KU (short for "where are yoU?") simplifies location permissions and fetching with a clean, composable API.

## ✨ Features

- 🎯 **Simple & Declarative** - One composable handles everything
- 🔒 **Permission Management** - Automatic permission handling with graceful fallbacks
- 🎨 **Fully Customizable** - Override any UI component
- 📱 **Kotlin Multiplatform** - Works on Android, iOS, and more
- ⚡ **Compose-First** - Built specifically for Jetpack/Compose Multiplatform
- 🛡️ **Type-Safe** - Leverages Kotlin's type system for reliability
- 🔄 **Smart Error Handling** - Shows stable location even when GPS signal is lost
- 🎛️ **Flexible Configuration** - Customizable update intervals, accuracy, and timeouts

## 🚀 Quick Start

### Installation

Add KU to your `commonMain` dependencies:

```kotlin
commonMain {
    dependencies {
        implementation("io.github.eltonkola:ku:0.0.3")
    }
}
```

## 📖 Usage Guide

### Simple Auto-Request

Start requesting location immediately when the composable loads:

```kotlin
LocationProvider(
    config = LocationConfig(autoRequest = true),
    onLocationReceived = { location ->
        Text("Location: ${location.format()}")
    }
)
```

### Manual Location Request

Let users control when to request location:

```kotlin
LocationProvider(
    onLocationReceived = { location ->
        Column {
            Text("Latitude: ${location.latitude}")
            Text("Longitude: ${location.longitude}")
            Text("Accuracy: ${location.accuracy}m")
        }
    },
    onInitial = { onRequestLocation ->
        Button(onClick = onRequestLocation) {
            Text("Get My Location")
        }
    }
)
```

### Configuration Options

Customize location behavior with `LocationConfig`:

```kotlin
LocationProvider(
    config = LocationConfig(
        autoRequest = true,
        singleRequest = false, // Set to true for one-time location
        highAccuracy = true, // Use GPS for high accuracy
        updateIntervalMs = 10_000L, // Update every 10 seconds
        minUpdateIntervalMs = 5_000L, // Minimum 5 seconds between updates
        timeoutMs = 30_000L // 30 second timeout
    ),
    onLocationReceived = { location ->
        MapView(location)
    }
)
```

### Custom UI Components

Override any part of the UI to match your design:

```kotlin
LocationProvider(
    onLocationReceived = { location ->
        LocationCard(location)
    },
    onInitial = { onRequestLocation ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onRequestLocation() }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Find My Location",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Tap to get your current position",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    },
    onLoading = {
        Card {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Finding your location...",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "This may take a few moments",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    },
    onPermissionDenied = { requestPermission ->
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.LocationOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Location Permission Required",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    "We need access to your location to show nearby places and services.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = requestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Grant Permission")
                }
            }
        }
    },
    onError = { errorMessage, retry ->
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Location Error",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = retry) {
                    Text("Try Again")
                }
            }
        }
    }
)
```

### Callback-Style Usage

For cases where you prefer callbacks over composable content:

```kotlin
LocationProvider(
    onLocationReceived = { location ->
        // Handle location update
        updateMapLocation(location)
    },
    onError = { error ->
        // Handle error
        showErrorToast(error)
    },
    onPermissionDenied = {
        // Handle permission denied
        showPermissionDialog()
    },
    config = LocationConfig(autoRequest = true)
)
```

### Error Handling Strategies

Handle different types of location errors:

```kotlin
LocationProvider(
    onError = { errorMessage, retry ->
        when {
            errorMessage.contains("GPS") || errorMessage.contains("Location services") -> {
                Column {
                    Text("GPS is disabled")
                    Text("Please enable location services in your device settings")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Button(onClick = { openLocationSettings() }) {
                            Text("Open Settings")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(onClick = retry) {
                            Text("Retry")
                        }
                    }
                }
            }
            errorMessage.contains("timeout") -> {
                Column {
                    Text("Location request timed out")
                    Text("Make sure you're not indoors and have a clear view of the sky")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = retry) {
                        Text("Try Again")
                    }
                }
            }
            else -> {
                Column {
                    Text("Location Error")
                    Text(errorMessage)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = retry) {
                        Text("Retry")
                    }
                }
            }
        }
    },
    onLocationReceived = { location ->
        MapView(location)
    }
)
```

### Hook-Style Usage

Use the `useLocation` hook for more control:

```kotlin
@Composable
fun MyLocationScreen() {
    val locationState = useLocation(
        config = LocationConfig(autoRequest = true, highAccuracy = true)
    )
    
    when (locationState) {
        LocationState.Loading -> {
            CircularProgressIndicator()
        }
        is LocationState.Success -> {
            LocationDetails(locationState.location)
        }
        is LocationState.Error -> {
            ErrorMessage(locationState.message)
        }
        LocationState.PermissionDenied -> {
            PermissionRequestUI()
        }
    }
}
```

## 🎨 API Reference

### LocationProvider Parameters

| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| `onLocationReceived` | `@Composable (Location) -> Unit` | Content displayed when location is successfully obtained | **Required** |
| `modifier` | `Modifier` | Modifier for the LocationProvider container | `Modifier` |
| `config` | `LocationConfig` | Configuration for location behavior | `LocationConfig()` |
| `onPermissionDenied` | `@Composable (() -> Unit) -> Unit` | Content displayed when permission is denied | Default permission UI |
| `onLoading` | `@Composable () -> Unit` | Content displayed while fetching location | Default loading UI |
| `onError` | `@Composable (String, () -> Unit) -> Unit` | Content displayed when an error occurs | Default error UI |
| `onInitial` | `@Composable (() -> Unit) -> Unit` | Content displayed before location request | Default button |

### LocationConfig

| Property | Type | Description | Default |
|----------|------|-------------|---------|
| `autoRequest` | `Boolean` | Whether to request location automatically | `false` |
| `singleRequest` | `Boolean` | Whether to request location only once | `false` |
| `highAccuracy` | `Boolean` | Whether to use high accuracy (GPS) | `true` |
| `updateIntervalMs` | `Long` | Interval between location updates (ms) | `10_000L` |
| `minUpdateIntervalMs` | `Long` | Minimum interval between updates (ms) | `5_000L` |
| `maxUpdateDelayMs` | `Long` | Maximum delay for location updates (ms) | `15_000L` |
| `timeoutMs` | `Long` | Timeout for location requests (ms) | `30_000L` |

### Location Data

```kotlin
data class Location(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float, // Accuracy in meters
    val altitude: Double?, // Altitude in meters (if available)
    val bearing: Float?, // Direction of travel (if available)
    val speed: Float?, // Speed in m/s (if available)
    val timestamp: Long, // Time when location was obtained
    val provider: String? // Location provider (GPS, Network, etc.)
)
```

### Location Extensions

```kotlin
// Format location coordinates
val formatted = location.format(precision = 4) // "40.7128, -74.0060"

// Calculate distance between two locations
val distance = location1.distanceTo(location2) // Distance in meters
```

### LocationState

```kotlin
sealed class LocationState {
    object Loading : LocationState()
    data class Success(val location: Location) : LocationState()
    data class Error(val message: String) : LocationState()
    object PermissionDenied : LocationState()
}
```

## 🔧 Advanced Features

### Smart Error Recovery

KU automatically handles location errors intelligently:

- **Before first location**: Shows error states to inform users
- **After first location**: Silently retries in background, keeps showing last known location
- **Graceful degradation**: Falls back to network location if GPS fails
- **Automatic recovery**: Resumes updates when location services become available

### Multiple Fallback Strategies

1. **Primary**: Current location with cancellation token
2. **Fallback 1**: Last known location (if recent)
3. **Fallback 2**: Short-duration location updates
4. **Fallback 3**: Network-based location

### Battery Optimization

- Configurable update intervals to balance accuracy vs battery life
- Automatic cleanup of location listeners
- Smart use of location providers based on accuracy requirements

## 📱 Platform Setup

### Android

Add location permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- Optional: For background location (if needed) -->
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

**ProGuard/R8 Rules** (if using code obfuscation):

```proguard
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.**
```

### iOS

Add location usage descriptions to your `Info.plist`:

```xml
<key>NSLocationWhenInUseUsageDescription</key>
<string>This app needs location access to show your current position and nearby places.</string>

<key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
<string>This app needs location access to provide location-based services.</string>
```

## 🧪 Testing

### Testing Location Functionality

```kotlin
@Test
fun testLocationProvider() {
    // Use Android Location Testing framework or iOS Location Simulator
    // KU integrates well with standard platform testing tools
}
```

### Mock Location Data

```kotlin
val mockLocation = Location(
    latitude = 40.7128,
    longitude = -74.0060,
    accuracy = 10.0f,
    altitude = 100.0,
    speed = null,
    bearing = null,
    timestamp = System.currentTimeMillis(),
    provider = "mock"
)
```

## 🐛 Troubleshooting

### Common Issues

**Location always returns null/error:**
- Ensure location permissions are granted
- Check if location services are enabled on device
- Test outdoors for better GPS signal
- Increase `timeoutMs` in LocationConfig

**UI keeps flashing between states:**
- This is fixed in the latest version
- Ensure you're using the updated LocationProvider API

**Battery drain:**
- Increase `updateIntervalMs` and `minUpdateIntervalMs`
- Set `singleRequest = true` if you only need one location
- Set `highAccuracy = false` for less precise but battery-friendly location

**iOS permission issues:**
- Ensure Info.plist entries are correctly formatted
- Test on physical device (simulator behavior may differ)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

### Development Setup

1. Clone the repository
2. Open in Android Studio or IntelliJ IDEA
3. Sync Gradle dependencies
4. Run sample apps to test changes

### Code Style

- Follow Kotlin coding conventions
- Add documentation for public APIs
- Include unit tests for new features

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