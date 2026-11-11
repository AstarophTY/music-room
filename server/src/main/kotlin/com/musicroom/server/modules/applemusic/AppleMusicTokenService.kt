package com.musicroom.server.modules.applemusic

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.musicroom.server.config.AppleMusicConfig
import com.musicroom.server.common.exceptions.HttpException
import io.ktor.http.HttpStatusCode
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64

class AppleMusicTokenService(private val config: AppleMusicConfig) {
    private val signingKey: ECPrivateKey by lazy {
        val pem = Files.readString(Path.of(config.privateKeyPath))
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .filterNot(Char::isWhitespace)
        KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem))) as ECPrivateKey
    }
    private var cachedToken: String? = null
    private var cachedAt: Instant? = null

    @Synchronized
    fun create(now: Instant = Instant.now()): String {
        val issued = cachedAt
        if (issued != null && !now.isBefore(issued) && now.isBefore(issued.plusSeconds(3300))) return requireNotNull(cachedToken)
        if (config.teamId.isBlank() || config.keyId.isBlank() || config.privateKeyPath.isBlank()) {
            throw HttpException(HttpStatusCode.ServiceUnavailable, "apple_music_unavailable", "Apple Music is not configured")
        }
        val key = try { signingKey } catch (e: Exception) {
            throw HttpException(HttpStatusCode.ServiceUnavailable, "apple_music_unavailable", "Apple Music signing key is unavailable")
        }
        val token = JWT.create().withKeyId(config.keyId).withIssuer(config.teamId).withIssuedAt(now)
            .withExpiresAt(now.plusSeconds(3600)).sign(Algorithm.ECDSA256(null, key))
        cachedAt = now
        cachedToken = token
        return token
    }
}
