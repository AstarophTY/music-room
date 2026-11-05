package com.musicroom.app.common

import androidx.compose.runtime.Composable
import com.musicroom.shared.dto.GeoPoint

/**
 * Reads where the user is (asking for the permission the first time), for events whose votes are limited to an
 * area. The function returns null when the position is unknown or refused; the provider itself is null when this
 * platform cannot give one.
 */
@Composable
expect fun rememberLocationProvider(): (suspend () -> GeoPoint?)?
