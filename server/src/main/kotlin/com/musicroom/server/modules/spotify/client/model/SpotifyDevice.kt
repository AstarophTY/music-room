package com.musicroom.server.modules.spotify.client.model

import kotlinx.serialization.Serializable

/** A device where Spotify can play (phone, computer, speaker). [id] is null for some restricted devices. */
@Serializable
data class SpotifyDevice(
    val id: String? = null,
    val isActive: Boolean = false,
    val isRestricted: Boolean = false,
    val name: String = "",
    val type: String = "",
)
