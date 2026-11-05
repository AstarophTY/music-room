package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** A device where Spotify can play. [isActive]: the one playing (or paused) now. */
@Serializable
data class DeviceResponse(
    val id: String,
    val name: String,
    val type: DeviceType = DeviceType.OTHER,
    val isActive: Boolean = false,
)
