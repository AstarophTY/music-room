package com.musicroom.server.modules.spotify.client

/** Spotify answered with an error. 401: token expired or revoked; 403: user not allowed; 429: rate limited. */
class SpotifyApiException(val status: Int, message: String) : RuntimeException(message)
