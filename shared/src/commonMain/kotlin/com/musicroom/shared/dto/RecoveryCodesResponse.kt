package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Single-use backup codes, shown once: they let the user in if the phone is lost. */
@Serializable
data class RecoveryCodesResponse(
    val recoveryCodes: List<String>,
    val user: UserResponse,
)
