package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** A track of the Spotify catalog, as the app needs it. [uri] is the Spotify URI used for playback. */
@Serializable
data class TrackResponse(
    val id: String,
    val name: String,
    val artists: List<String>,
    val album: String,
    val imageUrl: String? = null,
    val durationMs: Long,
    val explicit: Boolean = false,
    val spotifyUrl: String? = null,
    val uri: String,
)
