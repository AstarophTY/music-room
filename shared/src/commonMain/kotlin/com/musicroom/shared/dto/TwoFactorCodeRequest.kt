package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

@Serializable
data class TwoFactorCodeRequest(
    val code: String,
)
