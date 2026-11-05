package com.musicroom.server.config

/**
 * Accepted OAuth client IDs, checked against the `aud` claim of the ID tokens.
 * An empty list turns that provider off.
 */
data class OAuthConfig(
    val googleClientIds: List<String> = emptyList(),
    val appleClientIds: List<String> = emptyList(),
)
