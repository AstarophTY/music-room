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
    }
}
