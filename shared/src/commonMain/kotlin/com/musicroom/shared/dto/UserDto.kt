package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val displayName: String,
    val emailVerified: Boolean,
)
