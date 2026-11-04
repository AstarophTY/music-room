package com.musicroom.server.modules.spotify.client.model

import kotlinx.serialization.Serializable

/** Entry of a playlist. [track] is null for removed tracks. */
@Serializable
data class SpotifyPlaylistTrackItem(val track: SpotifyTrack? = null)
