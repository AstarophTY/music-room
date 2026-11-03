package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Signs in (or signs up) with an ID token obtained from the provider's SDK on the device. */
@Serializable
data class OAuthLoginRequest(
    val provider: OAuthProvider,
    val idToken: String,
    /** Required only when no account exists yet for this identity. */
    val acceptedTerms: Boolean = false,
    /** Optional: Apple only gives the user's name to the app, not in the ID token. */
    val displayName: String? = null,
)
