package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
    /** Must be true: the server refuses sign-ups that did not accept the terms of use. */
    val acceptedTerms: Boolean = false,
)
