package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val displayName: String,
    val emailVerified: Boolean,
    val twoFactorEnabled: Boolean = false,
    val avatarColor: AvatarColor = AvatarColor.YELLOW,
    /** Changes each time the photo changes (cache key). Null when the user has no photo. */
    val avatarVersion: Long? = null,
)
