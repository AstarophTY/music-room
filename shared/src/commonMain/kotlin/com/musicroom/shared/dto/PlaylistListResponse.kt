package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** The Music Room playlists the user owns or was added to, most recently changed first. */
@Serializable
data class PlaylistListResponse(val items: List<PlaylistResponse>)
