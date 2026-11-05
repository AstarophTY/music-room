package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** An artist's page: their popular tracks and their albums and singles, newest first. */
@Serializable
data class ArtistDetailResponse(
    val artist: ArtistResponse,
    val tracks: List<TrackResponse> = emptyList(),
    val albums: List<AlbumResponse> = emptyList(),
)
