package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** What Spotify is playing on the user's devices. [active] is false when no device is playing or paused. */
@Serializable
data class PlaybackStateResponse(
    val active: Boolean,
    val isPlaying: Boolean = false,
    val track: TrackResponse? = null,
    val progressMs: Long = 0,
    val deviceName: String? = null,
)
