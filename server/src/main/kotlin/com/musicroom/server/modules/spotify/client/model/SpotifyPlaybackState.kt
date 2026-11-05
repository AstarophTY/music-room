package com.musicroom.server.modules.spotify.client.model

import kotlinx.serialization.Serializable

/**
 * Answer of GET /me/player (and of GET /me/player/currently-playing, without [device]). [item] is null during
 * ads or for unknown content ([currentlyPlayingType] "ad" or "unknown").
 */
@Serializable
data class SpotifyPlaybackState(
    val device: SpotifyDevice? = null,
    val isPlaying: Boolean = false,
    val progressMs: Long? = null,
    val item: SpotifyTrack? = null,
    val currentlyPlayingType: String? = null,
)
