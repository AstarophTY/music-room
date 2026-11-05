package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Kind of Spotify device, to show a fitting icon. */
@Serializable
enum class DeviceType {
    COMPUTER,
    SMARTPHONE,
    TABLET,
    SPEAKER,
    TV,
    CAR,
    OTHER,
}
