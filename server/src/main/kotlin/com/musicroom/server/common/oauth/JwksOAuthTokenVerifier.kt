package com.musicroom.server.common.oauth

import com.auth0.jwk.JwkException
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.Claim
import com.musicroom.server.common.exceptions.BadRequestException
import com.musicroom.server.common.exceptions.UnauthorizedException
import com.musicroom.server.config.OAuthConfig
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.dto.OAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit

/**
 * Verifies Google and Apple ID tokens on the server, with the providers' public keys (JWKS).
 * The app's word is never trusted: only a token signed by the provider for one of our client IDs is accepted.
 */
class JwksOAuthTokenVerifier(config: OAuthConfig) : OAuthTokenVerifier {

    private val providers = mapOf(
        OAuthProvider.GOOGLE to Provider(
            jwksUrl = "https://www.googleapis.com/oauth2/v3/certs",
            issuers = listOf("https://accounts.google.com", "accounts.google.com"),
            audiences = config.googleClientIds,
        ),
        OAuthProvider.APPLE to Provider(
            jwksUrl = "https://appleid.apple.com/auth/keys",
            issuers = listOf("https://appleid.apple.com"),
            audiences = config.appleClientIds,
        ),
    )

    override suspend fun verify(provider: OAuthProvider, idToken: String): OAuthIdentity {
        val settings = providers.getValue(provider)
        if (settings.audiences.isEmpty()) {
            throw BadRequestException(
                "${provider.displayName} sign-in is not configured on the server",
                ApiErrors.OAUTH_NOT_CONFIGURED,
            )
        }
        val token = withContext(Dispatchers.IO) {
            try {
                val decoded = JWT.decode(idToken)
                val publicKey = settings.keys.get(decoded.keyId).publicKey as RSAPublicKey
                JWT.require(Algorithm.RSA256(publicKey, null))
                    .withIssuer(*settings.issuers.toTypedArray())
                    .withAnyOfAudience(*settings.audiences.toTypedArray())
                    .acceptLeeway(CLOCK_SKEW_SECONDS)
                    .build()
                    .verify(decoded)
            } catch (e: JWTVerificationException) {
                throw invalidToken(provider)
            } catch (e: JwkException) {
                throw invalidToken(provider)
            }
        }
        return OAuthIdentity(
            provider = provider,
            subject = token.subject ?: throw invalidToken(provider),
            email = token.getClaim("email").asString(),
            emailVerified = token.getClaim("email_verified").asFlag(),
            name = token.getClaim("name").asString(),
        )
    }

    private fun invalidToken(provider: OAuthProvider) =
        UnauthorizedException("Invalid ${provider.displayName} token", ApiErrors.INVALID_OAUTH_TOKEN)

    /** Google sends a JSON boolean, Apple sometimes sends the string "true". */
    private fun Claim.asFlag(): Boolean = asBoolean() ?: asString()?.toBooleanStrictOrNull() ?: false

    private class Provider(jwksUrl: String, val issuers: List<String>, val audiences: List<String>) {
        /** Public keys are cached: the providers rotate them rarely. */
        val keys: JwkProvider = JwkProviderBuilder(URI(jwksUrl).toURL())
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()
    }

    private companion object {
        const val CLOCK_SKEW_SECONDS = 60L
    }
}
