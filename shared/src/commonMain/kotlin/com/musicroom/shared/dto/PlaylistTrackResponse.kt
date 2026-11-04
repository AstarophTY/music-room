package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** A song in a playlist. [entryId] identifies this entry, to remove it. */
@Serializable
data class PlaylistTrackResponse(
    val entryId: String,
    val track: TrackResponse,
    val addedBy: String,
)
