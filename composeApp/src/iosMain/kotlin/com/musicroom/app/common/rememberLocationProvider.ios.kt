package com.musicroom.app.common

import androidx.compose.runtime.Composable
import com.musicroom.shared.dto.GeoPoint

/** Not wired to Core Location yet: area-limited votes are not available on iOS. */
@Composable
actual fun rememberLocationProvider(): (suspend () -> GeoPoint?)? = null
