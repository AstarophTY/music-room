package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Search results of each kind. More tracks come from the track search, from [nextTrackOffset]. */
@Serializable
data class SearchResponse(
    val tracks: List<TrackResponse> = emptyList(),
    val artists: List<ArtistResponse> = emptyList(),
    val albums: List<AlbumResponse> = emptyList(),
    val nextTrackOffset: Int? = null,
)
