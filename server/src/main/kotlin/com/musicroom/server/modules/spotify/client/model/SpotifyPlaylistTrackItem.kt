package com.musicroom.server.modules.spotify.client.model

import kotlinx.serialization.Serializable

/** Entry of a playlist: [item] since the February 2026 API changes, [track] before. Null for removed tracks. */
@Serializable
data class SpotifyPlaylistTrackItem(val item: SpotifyTrack? = null, val track: SpotifyTrack? = null) {
    val content: SpotifyTrack? get() = item ?: track
}
