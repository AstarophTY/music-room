package com.musicroom.server.config

/** Spotify app credentials. Empty [clientId]: Spotify is not configured and music routes answer 503. */
data class SpotifyConfig(
    val clientId: String = "",
    val clientSecret: String = "",
    val redirectUri: String = "http://127.0.0.1:8080/api/v1/spotify/callback",
) {
    val configured: Boolean get() = clientId.isNotEmpty() && clientSecret.isNotEmpty()
}
