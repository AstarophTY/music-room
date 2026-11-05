package com.musicroom.server.common.oauth

import com.musicroom.shared.dto.OAuthProvider

/** Who the provider says the user is, read from a verified ID token. */
data class OAuthIdentity(
    val provider: OAuthProvider,
    /** Stable user ID at the provider (`sub` claim). Emails can change, this cannot. */
    val subject: String,
    val email: String?,
    val emailVerified: Boolean,
    val name: String?,
)
