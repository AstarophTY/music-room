package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** The user's devices with Spotify open, the active one first. */
@Serializable
data class DevicesResponse(val devices: List<DeviceResponse> = emptyList())
