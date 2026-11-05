package com.musicroom.server.modules.spotify.client.model

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyImage(val url: String, val width: Int? = null, val height: Int? = null)
