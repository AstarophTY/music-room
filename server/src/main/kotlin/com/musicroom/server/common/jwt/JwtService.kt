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

    fun createAccessToken(userId: UUID, now: Instant = Instant.now()): String = JWT.create()
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .withSubject(userId.toString())
        .withIssuedAt(now)
        .withExpiresAt(now.plusSeconds(accessTokenTtlSeconds))
        .sign(algorithm)
}
