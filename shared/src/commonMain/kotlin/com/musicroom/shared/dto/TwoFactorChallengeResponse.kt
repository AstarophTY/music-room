package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Sent with HTTP 202 instead of tokens when the account has 2FA: the app must ask for a code. */
@Serializable
data class TwoFactorChallengeResponse(
    val challengeToken: String,
    val expiresInSeconds: Long,
)
