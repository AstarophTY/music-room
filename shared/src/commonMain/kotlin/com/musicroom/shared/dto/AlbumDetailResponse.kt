package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** An album's page: the album and its tracks in order. */
@Serializable
data class AlbumDetailResponse(
    val album: AlbumResponse,
    val tracks: List<TrackResponse> = emptyList(),
)
