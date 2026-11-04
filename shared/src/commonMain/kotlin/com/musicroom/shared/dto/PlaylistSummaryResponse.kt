package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistSummaryResponse(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val trackCount: Int? = null,
    val owner: String? = null,
    val spotifyUrl: String? = null,
)
