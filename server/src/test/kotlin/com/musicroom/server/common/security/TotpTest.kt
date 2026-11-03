package com.musicroom.server.common.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TotpTest {
    /** RFC 6238 test vector: secret "12345678901234567890", time 59 s -> 94287082 (last 6 digits: 287082). */
    private val rfcSecret = Totp.base32Encode("12345678901234567890".toByteArray())

    @Test
    fun `matches the RFC 6238 test vector`() {
        assertEquals("287082", Totp.code(rfcSecret, Totp.step(59)))
        assertEquals("081804", Totp.code(rfcSecret, Totp.step(1111111109)))
    }

    @Test
    fun `base32 round trip`() {
        val bytes = byteArrayOf(0, 1, 2, -1, 127, -128, 42)
        assertEquals(bytes.toList(), Totp.base32Decode(Totp.base32Encode(bytes)).toList())
    }

    @Test
    fun `verify accepts the drift window and refuses replays`() {
        val now = 1_700_000_000L
        val step = Totp.step(now)
        assertEquals(step, Totp.verify(rfcSecret, Totp.code(rfcSecret, step), now, lastUsedStep = null))
        assertEquals(step + 1, Totp.verify(rfcSecret, Totp.code(rfcSecret, step + 1), now, lastUsedStep = null))
        assertNull(Totp.verify(rfcSecret, Totp.code(rfcSecret, step + 2), now, lastUsedStep = null))
        assertNull(Totp.verify(rfcSecret, Totp.code(rfcSecret, step), now, lastUsedStep = step))
    }

    @Test
    fun `secret box round trip`() {
        val box = SecretBox("x".repeat(32))
        val encrypted = box.encrypt("JBSWY3DPEHPK3PXP")
        assertEquals("JBSWY3DPEHPK3PXP", box.decrypt(encrypted))
    }
}
