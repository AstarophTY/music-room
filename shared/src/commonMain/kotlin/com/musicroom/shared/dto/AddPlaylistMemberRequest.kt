package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Adds the Music Room user with this [email] to the playlist. */
@Serializable
data class AddPlaylistMemberRequest(val email: String, val access: PlaylistAccess = PlaylistAccess.READ)
