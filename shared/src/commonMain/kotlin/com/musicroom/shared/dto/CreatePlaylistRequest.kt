package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** By default a playlist is public and everyone can edit it. */
@Serializable
data class CreatePlaylistRequest(
    val name: String,
    val visibility: PlaylistVisibility = PlaylistVisibility.PUBLIC,
    val editLicense: PlaylistEditLicense = PlaylistEditLicense.EVERYONE,
)
