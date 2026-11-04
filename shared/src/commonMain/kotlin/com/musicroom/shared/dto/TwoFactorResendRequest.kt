package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Asks for a new emailed code during a login with email 2FA. */
@Serializable
data class TwoFactorResendRequest(
    val challengeToken: String,
)
