package com.musicroom.server.modules.spotify.client.model

import kotlinx.serialization.Serializable

/** Answer of the Spotify token endpoint. A refresh may not include a new refresh token. */
@Serializable
data class SpotifyTokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val scope: String? = null,
    val expiresIn: Long,
    val refreshToken: String? = null,
)
