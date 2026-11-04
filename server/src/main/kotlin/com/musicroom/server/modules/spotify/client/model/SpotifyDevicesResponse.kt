package com.musicroom.server.modules.spotify.client.model

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyDevicesResponse(val devices: List<SpotifyDevice> = emptyList())
