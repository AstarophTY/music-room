package com.musicroom.server.modules.spotify.client.model

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyAlbum(val name: String = "", val images: List<SpotifyImage> = emptyList())
