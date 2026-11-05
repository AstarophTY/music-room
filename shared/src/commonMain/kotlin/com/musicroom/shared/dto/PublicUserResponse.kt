package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** What anyone can see of a user, and how they relate to the user who asks. */
@Serializable
data class PublicUserResponse(
    val id: String,
    val displayName: String,
    val avatarColor: AvatarColor = AvatarColor.YELLOW,
    /** Same as [UserResponse.avatarVersion]: null when the user has no photo. */
    val avatarVersion: Long? = null,
    val friendship: FriendshipStatus = FriendshipStatus.NONE,
)
