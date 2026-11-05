package com.musicroom.server.modules.applemusic

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.musicroom.server.config.AppleMusicConfig
import java.nio.file.Files
import java.security.KeyPairGenerator
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.time.Instant
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals

class AppleMusicTokenServiceTest {
    @Test
    fun `token is signed with configured key and expires after one hour`() {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec("secp256r1"))
        val pair = generator.generateKeyPair()
        val file = Files.createTempFile("music-kit-test", ".p8")
        try {
            Files.writeString(file, "-----BEGIN PRIVATE KEY-----\n${Base64.getEncoder().encodeToString(pair.private.encoded)}\n-----END PRIVATE KEY-----\n")
            val now = Instant.now()
            val token = AppleMusicTokenService(AppleMusicConfig("team", "key", file.toString())).create(now)
            val verified = JWT.require(Algorithm.ECDSA256(pair.public as ECPublicKey, null)).withIssuer("team").build().verify(token)
            assertEquals("key", verified.keyId)
            assertEquals(now.epochSecond + 3600, verified.expiresAtAsInstant.epochSecond)
        } finally {
            Files.deleteIfExists(file)
        }
    }
}
