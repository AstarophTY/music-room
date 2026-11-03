package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Ends a login that needs 2FA. [code]: 6 digits from the authenticator app, or a recovery code. */
@Serializable
data class TwoFactorLoginRequest(
    val challengeToken: String,
    val code: String,
)
