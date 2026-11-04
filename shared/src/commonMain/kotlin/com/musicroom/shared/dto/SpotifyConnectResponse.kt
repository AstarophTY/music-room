package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Spotify login page to open in the browser. Spotify then sends the user back to the server. */
@Serializable
data class SpotifyConnectResponse(
    val authorizeUrl: String,
)
