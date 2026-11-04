package com.musicroom.server.common.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.musicroom.server.config.JwtConfig
import java.time.Instant
import java.util.UUID

class JwtService(private val config: JwtConfig) {

    private val algorithm = Algorithm.HMAC256(config.secret)

    val realm: String get() = config.realm

    val accessTokenTtlSeconds: Long get() = config.accessTokenTtlMinutes * 60

    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

    /** Accepts only 2FA challenge tokens: their audience differs, so they never work as access tokens. */
    private val challengeVerifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(config.issuer)
        .withAudience(CHALLENGE_AUDIENCE)
        .build()

    /** Short-lived proof that the password (or OAuth) step of a login succeeded; the 2FA code comes next. */
    fun createTwoFactorChallenge(userId: UUID, now: Instant = Instant.now()): String = JWT.create()
        .withIssuer(config.issuer)
        .withAudience(CHALLENGE_AUDIENCE)
        .withSubject(userId.toString())
        .withIssuedAt(now)
        .withExpiresAt(now.plusSeconds(CHALLENGE_TTL_SECONDS))
        .sign(algorithm)

    fun verifyTwoFactorChallenge(token: String): UUID? =
        runCatching { UUID.fromString(challengeVerifier.verify(token).subject) }.getOrNull()

    private val spotifyStateVerifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(config.issuer)
        .withAudience(SPOTIFY_STATE_AUDIENCE)
        .build()

    /** OAuth "state" for the Spotify login: signed (no forgery, no CSRF), short-lived, and says which user it is for. */
    fun createSpotifyState(userId: UUID, now: Instant = Instant.now()): String = JWT.create()
        .withIssuer(config.issuer)
        .withAudience(SPOTIFY_STATE_AUDIENCE)
        .withSubject(userId.toString())
        .withIssuedAt(now)
        .withExpiresAt(now.plusSeconds(SPOTIFY_STATE_TTL_SECONDS))
        .sign(algorithm)

    fun verifySpotifyState(token: String): UUID? =
        runCatching { UUID.fromString(spotifyStateVerifier.verify(token).subject) }.getOrNull()

    fun createAccessToken(userId: UUID, now: Instant = Instant.now()): String = JWT.create()
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .withSubject(userId.toString())
        .withIssuedAt(now)
        .withExpiresAt(now.plusSeconds(accessTokenTtlSeconds))
        .sign(algorithm)

    companion object {
        const val CHALLENGE_AUDIENCE = "music-room-2fa"
        const val CHALLENGE_TTL_SECONDS = 300L
        const val SPOTIFY_STATE_AUDIENCE = "music-room-spotify-state"
        const val SPOTIFY_STATE_TTL_SECONDS = 600L
    }
}
