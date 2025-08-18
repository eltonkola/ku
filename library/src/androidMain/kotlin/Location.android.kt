package com.eltonkola.ku

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.Flow

actual class LocationClient {

    private var context: Context? = null

    actual fun initialize(context: Any?) {
        require(context is Context) { "Android implementation requires Context" }
        this.context = context
    }

    actual fun getLocation(): Flow<LocationState> {
        TODO("Not yet implemented")
    }

    actual fun hasPermission(): Boolean {
        TODO("Not yet implemented")
    }

    actual fun requestPermission() {
    }
    actual fun onDispose() {
    }
}
actual val currentPlatform: PlatformType
    get() = PlatformType.ANDROID


@Composable
actual fun getPlatformContext(): Any? = LocalContext.current