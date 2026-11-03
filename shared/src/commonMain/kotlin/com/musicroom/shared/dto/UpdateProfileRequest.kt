package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequest(
    val displayName: String,
)
