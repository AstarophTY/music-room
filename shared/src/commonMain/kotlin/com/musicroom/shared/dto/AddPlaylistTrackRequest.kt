package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** [trackId]: Spotify track id; the server reads the track details from Spotify. */
@Serializable
data class AddPlaylistTrackRequest(val trackId: String)
