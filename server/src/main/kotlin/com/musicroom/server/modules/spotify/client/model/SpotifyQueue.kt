package com.musicroom.server.modules.spotify.client.model

import kotlinx.serialization.Serializable

/** Answer of GET /me/player/queue: what plays now and the next tracks (Spotify lists about 20). */
@Serializable
data class SpotifyQueue(
    val currentlyPlaying: SpotifyTrack? = null,
    val queue: List<SpotifyTrack> = emptyList(),
)
