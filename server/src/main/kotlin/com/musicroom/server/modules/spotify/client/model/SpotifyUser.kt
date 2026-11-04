package com.musicroom.server.modules.spotify.client.model

import kotlinx.serialization.Serializable

/** The connected Spotify account. [product]: "premium", "free"... */
@Serializable
data class SpotifyUser(val id: String, val displayName: String? = null, val product: String? = null)
