package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Null fields are left unchanged. */
@Serializable
data class UpdatePlaylistRequest(
    val name: String? = null,
    val visibility: PlaylistVisibility? = null,
    val editLicense: PlaylistEditLicense? = null,
)
