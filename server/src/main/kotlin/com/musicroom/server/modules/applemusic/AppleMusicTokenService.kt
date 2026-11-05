package com.musicroom.server.modules.applemusic

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.musicroom.server.config.AppleMusicConfig
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64

class AppleMusicTokenService(private val config: AppleMusicConfig) {
    fun create(now: Instant = Instant.now()): String {
        require(config.teamId.isNotBlank() && config.keyId.isNotBlank() && config.privateKeyPath.isNotBlank()) {
            "Apple Music is not configured"
        }
        val pem = Files.readString(Path.of(config.privateKeyPath))
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .filterNot(Char::isWhitespace)
        val key = KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem))) as ECPrivateKey
        return JWT.create().withKeyId(config.keyId).withIssuer(config.teamId).withIssuedAt(now)
            .withExpiresAt(now.plusSeconds(3600)).sign(Algorithm.ECDSA256(null, key))
    }
}
