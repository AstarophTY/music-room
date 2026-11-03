package com.musicroom.app.auth

/** What a provider's SDK returns on the device. The server verifies [idToken]. */
data class SocialCredential(
    val idToken: String,
    val displayName: String?,
)
