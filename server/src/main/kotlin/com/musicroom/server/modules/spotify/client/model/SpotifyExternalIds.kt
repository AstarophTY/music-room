package com.musicroom.server.modules.spotify.client.model

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyExternalIds(val isrc: String? = null)
