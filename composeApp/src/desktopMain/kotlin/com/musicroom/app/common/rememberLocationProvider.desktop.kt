package com.musicroom.app.common

import androidx.compose.runtime.Composable
import com.musicroom.shared.dto.GeoPoint

/** Computers have no position service the app can use. */
@Composable
actual fun rememberLocationProvider(): (suspend () -> GeoPoint?)? = null
