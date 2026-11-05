package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** New secret to add to an authenticator app: scan [otpauthUri] as a QR code, or type [secret]. */
@Serializable
data class TwoFactorSetupResponse(
    val secret: String,
    val otpauthUri: String,
)
