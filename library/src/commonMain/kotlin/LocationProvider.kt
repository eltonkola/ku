package com.eltonkola.ku

@Composable
fun LocationProvider(
    permissionDeniedContent: @Composable (() -> Unit) -> Unit = { requestPermission ->
        DefaultPermissionDeniedContent(onRequestPermission = requestPermission)
    },
    loadingContent: @Composable () -> Unit = { DefaultLoadingContent() },
    errorContent: @Composable (String) -> Unit = { DefaultErrorContent(it) },
    content: @Composable (Location) -> Unit
) {
    val locationClient = rememberLocationClient()
    var shouldRequestLocation by remember { mutableStateOf(false) }
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
    }

    Button(onClick = { shouldRequestLocation = true }) {
        Text("Get My Location")
    }
}

@Composable
private fun rememberLocationClient(): LocationClient {
    val client = remember { LocationClient() }

    if (Platform.isAndroid()) {
        val context = LocalContext.current
        DisposableEffect(client) {
            (client as? AndroidLocationClient)?.setContext(context)
            onDispose { }
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
        Text("Error: $message", color = MaterialTheme.colors.error)
    }
}