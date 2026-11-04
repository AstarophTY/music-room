package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** A Spotify playlist the user created. [uri] plays it as a whole. */
@Serializable
data class PlaylistSummaryResponse(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val trackCount: Int? = null,
    val owner: String? = null,
    val spotifyUrl: String? = null,
    val uri: String? = null,
)
