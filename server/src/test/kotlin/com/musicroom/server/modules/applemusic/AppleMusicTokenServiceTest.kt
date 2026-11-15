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
import kotlin.test.assertNotEquals
import kotlin.test.assertFailsWith
import com.musicroom.server.common.exceptions.HttpException
import io.ktor.http.HttpStatusCode

class AppleMusicTokenServiceTest {
    @Test
    fun `missing configuration is unavailable`() {
        val error = assertFailsWith<HttpException> {
            AppleMusicTokenService(AppleMusicConfig("", "", "")).create()
        }
        assertEquals(HttpStatusCode.ServiceUnavailable, error.status)
    }

    @Test
    fun `token is cached and key survives renewal`() {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec("secp256r1"))
        val pair = generator.generateKeyPair()
        val file = Files.createTempFile("music-kit-cache", ".p8")
        try {
            Files.writeString(file, "-----BEGIN PRIVATE KEY-----\n${Base64.getEncoder().encodeToString(pair.private.encoded)}\n-----END PRIVATE KEY-----\n")
            val service = AppleMusicTokenService(AppleMusicConfig("team", "key", file.toString()))
            val now = Instant.now()
            val first = service.create(now)
            Files.delete(file)
            assertEquals(first, service.create(now.plusSeconds(3299)))
            assertNotEquals(first, service.create(now.plusSeconds(3300)))
        } finally {
            Files.deleteIfExists(file)
        }
    }

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
