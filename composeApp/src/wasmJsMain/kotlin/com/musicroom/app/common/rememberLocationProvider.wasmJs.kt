package com.musicroom.app.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.musicroom.shared.dto.GeoPoint
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** Browser geolocation (the browser asks the user the first time). */
@Composable
actual fun rememberLocationProvider(): (suspend () -> GeoPoint?)? = remember {
    {
        suspendCancellableCoroutine { continuation ->
            requestBrowserPosition(
                onFound = { latitude, longitude -> if (continuation.isActive) continuation.resume(GeoPoint(latitude, longitude)) },
                onFailed = { if (continuation.isActive) continuation.resume(null) },
            )
        }
    }
}
