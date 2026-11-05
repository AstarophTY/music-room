package com.musicroom.app.auth

import com.musicroom.shared.dto.OAuthProvider

/** Native Google / Apple sign-in, implemented by each platform (the SDKs are platform-specific). */
interface SocialSignIn {
    /** Providers available on this device, in display order. */
    val providers: List<OAuthProvider>

    /**
     * Shows the provider's sign-in sheet. Returns null if the user closed it.
     * Throws [SocialSignInException] if sign-in failed.
     */
    suspend fun signIn(provider: OAuthProvider): SocialCredential?

    companion object {
        /** For platforms without native sign-in yet: no buttons are shown. */
        val Unavailable: SocialSignIn = object : SocialSignIn {
            override val providers: List<OAuthProvider> = emptyList()
            override suspend fun signIn(provider: OAuthProvider): SocialCredential? = null
        }
    }
}
