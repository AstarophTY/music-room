package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** A playlist with its songs in order and its members (the owner is in [PlaylistResponse.owner]). */
@Serializable
data class PlaylistDetailResponse(
    val playlist: PlaylistResponse,
    val tracks: List<PlaylistTrackResponse>,
    val members: List<PlaylistMemberResponse>,
)
