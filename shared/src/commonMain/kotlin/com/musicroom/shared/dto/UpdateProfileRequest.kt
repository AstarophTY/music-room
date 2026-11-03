package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Fields left null are not changed. */
@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val avatarColor: AvatarColor? = null,
)
