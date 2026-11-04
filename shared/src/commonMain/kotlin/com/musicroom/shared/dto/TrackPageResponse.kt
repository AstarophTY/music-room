package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** One page of tracks. [nextOffset] is null on the last page. */
@Serializable
data class TrackPageResponse(
    val items: List<TrackResponse>,
    val total: Int,
    val nextOffset: Int? = null,
)
