package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/**
 * A song in a playlist. [entryId] identifies this entry, to remove it. [addedBy] is the display name of who
 * proposed it, [addedById] their id (null when that account was deleted).
 */
@Serializable
data class PlaylistTrackResponse(
    val entryId: String,
    val track: TrackResponse,
    val addedBy: String,
    val addedById: String? = null,
)
