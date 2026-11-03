package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

@Serializable
data class VerifyEmailRequest(
    val code: String,
)
