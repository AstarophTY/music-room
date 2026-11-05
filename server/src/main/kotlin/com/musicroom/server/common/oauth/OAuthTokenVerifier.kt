package com.musicroom.server.common.oauth

import com.musicroom.shared.dto.OAuthProvider

fun interface OAuthTokenVerifier {
    /** Checks the ID token's signature, issuer, audience and expiry. Throws an HttpException if it is not valid. */
    suspend fun verify(provider: OAuthProvider, idToken: String): OAuthIdentity
}
