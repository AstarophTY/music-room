package com.musicroom.server.modules.spotify.client.model

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyArtist(val id: String? = null, val name: String)
