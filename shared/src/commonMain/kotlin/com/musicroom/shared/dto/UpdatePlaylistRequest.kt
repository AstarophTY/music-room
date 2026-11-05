package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Null fields are left unchanged. Turning a playlist [PlaylistType.PERSONAL] makes every member read-only. */
@Serializable
data class UpdatePlaylistRequest(val name: String? = null, val type: PlaylistType? = null)
