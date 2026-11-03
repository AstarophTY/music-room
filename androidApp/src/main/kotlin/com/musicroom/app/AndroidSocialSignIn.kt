package com.musicroom.app

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.musicroom.app.auth.SocialCredential
import com.musicroom.app.auth.SocialSignIn
import com.musicroom.app.auth.SocialSignInException
import com.musicroom.shared.dto.OAuthProvider

/**
 * Google sign-in with Android's Credential Manager. Sign in with Apple has no Android SDK, so it is not offered.
 * [googleClientId] is the "Web application" OAuth client ID: the ID token is issued for it, and the server checks it.
 */
class AndroidSocialSignIn(
    private val activity: Activity,
    private val googleClientId: String,
) : SocialSignIn {
    private val credentialManager = CredentialManager.create(activity)

    override val providers: List<OAuthProvider> = listOf(OAuthProvider.GOOGLE)

    override suspend fun signIn(provider: OAuthProvider): SocialCredential? {
        if (provider != OAuthProvider.GOOGLE) throw SocialSignInException("${provider.displayName} sign-in is not available on Android")
        if (googleClientId.isBlank()) {
            throw SocialSignInException(
                "Google sign-in is not configured: set GOOGLE_CLIENT_ID in .env and rebuild the app",
                notConfigured = true,
            )
        }
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId = googleClientId).build())
            .build()
        val credential = try {
            credentialManager.getCredential(activity, request).credential
        } catch (e: GetCredentialCancellationException) {
            return null
        } catch (e: GetCredentialException) {
            throw SocialSignInException("Google sign-in failed: ${e.message ?: e.type}")
        }
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw SocialSignInException("Google sign-in returned an unexpected credential")
        }
        return try {
            val google = GoogleIdTokenCredential.createFrom(credential.data)
            SocialCredential(idToken = google.idToken, displayName = google.displayName)
        } catch (e: GoogleIdTokenParsingException) {
            throw SocialSignInException("Google sign-in returned an invalid token")
        }
    }
}
