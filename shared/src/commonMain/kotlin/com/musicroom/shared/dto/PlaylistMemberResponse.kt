package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** A user of a playlist, with what they can do. The owner always has [PlaylistAccess.WRITE]. */
@Serializable
data class PlaylistMemberResponse(
    val userId: String,
    val displayName: String,
    val access: PlaylistAccess,
    val avatarColor: AvatarColor = AvatarColor.YELLOW,
    /** Same as [UserResponse.avatarVersion]: null when the user has no photo. */
    val avatarVersion: Long? = null,
)
