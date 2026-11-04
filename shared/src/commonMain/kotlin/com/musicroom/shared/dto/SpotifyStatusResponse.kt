package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** [configured]: the server has Spotify credentials. [product]: "premium" or "free" (playback control needs premium). */
@Serializable
data class SpotifyStatusResponse(
    val configured: Boolean,
    val connected: Boolean,
    val displayName: String? = null,
    val product: String? = null,
)
