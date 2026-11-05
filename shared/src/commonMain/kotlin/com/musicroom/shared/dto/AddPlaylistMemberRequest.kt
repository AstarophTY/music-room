package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Adds a user to the playlist: one of the owner's friends ([userId]), or the Music Room user with this [email]. */
@Serializable
data class AddPlaylistMemberRequest(
    val email: String = "",
    val access: PlaylistAccess = PlaylistAccess.READ,
    val userId: String? = null,
)
