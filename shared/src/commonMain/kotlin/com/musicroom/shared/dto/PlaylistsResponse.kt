package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistsResponse(
    val items: List<PlaylistSummaryResponse>,
)
