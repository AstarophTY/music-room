package com.musicroom.app.common

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.musicroom.shared.dto.GeoPoint
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Android location: asks for the permission when needed, then reads a fresh position (or the last known one on
 * Android before 11, or when no fresh one comes within a few seconds).
 */
@SuppressLint("MissingPermission")
@Composable
actual fun rememberLocationProvider(): (suspend () -> GeoPoint?)? {
    val context = LocalContext.current
    val answer = remember { arrayOf<CompletableDeferred<Boolean>?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        answer[0]?.complete(granted.values.any { it })
    }
    return remember(context) {
        provider@{
            val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            val granted = permissions.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED } || run {
                val pending = CompletableDeferred<Boolean>()
                answer[0] = pending
                launcher.launch(permissions)
                pending.await()
            }
            if (!granted) return@provider null
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@provider null
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter { manager.isProviderEnabled(it) }
            val fresh = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && providers.isNotEmpty()) {
                withTimeoutOrNull(8_000) {
                    suspendCancellableCoroutine<Location?> { continuation ->
                        val signal = CancellationSignal()
                        continuation.invokeOnCancellation { signal.cancel() }
                        manager.getCurrentLocation(providers.first(), signal, ContextCompat.getMainExecutor(context)) {
                            if (continuation.isActive) continuation.resume(it)
                        }
                    }
                }
            } else {
                null
            }
            val location = fresh ?: providers.mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.time }
            location?.let { GeoPoint(it.latitude, it.longitude) }
        }
    }
}
