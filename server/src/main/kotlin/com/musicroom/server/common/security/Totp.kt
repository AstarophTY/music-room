package com.musicroom.server.common.security

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Time-based one-time passwords (RFC 6238), the codes of authenticator apps:
 * HMAC-SHA1, 6 digits, 30-second steps. Secrets are exchanged in Base32 (RFC 4648).
 */
object Totp {
    const val DIGITS = 6
    const val STEP_SECONDS = 30L
    private const val SECRET_BYTES = 20
    private const val BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private val random = SecureRandom()

    fun generateSecret(): String = base32Encode(ByteArray(SECRET_BYTES).also(random::nextBytes))

    fun step(epochSeconds: Long): Long = epochSeconds / STEP_SECONDS

    fun code(secret: String, step: Long): String {
        val mac = Mac.getInstance("HmacSHA1").apply { init(SecretKeySpec(base32Decode(secret), "HmacSHA1")) }
        val hash = mac.doFinal(ByteBuffer.allocate(Long.SIZE_BYTES).putLong(step).array())
        val offset = hash.last().toInt() and 0x0f
        val binary = ((hash[offset].toInt() and 0x7f) shl 24) or
            ((hash[offset + 1].toInt() and 0xff) shl 16) or
            ((hash[offset + 2].toInt() and 0xff) shl 8) or
            (hash[offset + 3].toInt() and 0xff)
        return (binary % 1_000_000).toString().padStart(DIGITS, '0')
    }

    /**
     * Returns the step [code] belongs to, or null. Accepts one step before and after (clock drift),
     * and never a step at or before [lastUsedStep] (no replay).
     */
    fun verify(secret: String, code: String, epochSeconds: Long, lastUsedStep: Long?): Long? {
        if (code.length != DIGITS || !code.all { it in '0'..'9' }) return null
        val now = step(epochSeconds)
        return (now - 1..now + 1).firstOrNull { candidate ->
            (lastUsedStep == null || candidate > lastUsedStep) && Sha256.matches(code(secret, candidate), Sha256.hex(code))
        }
    }

    /** Link that authenticator apps understand (also encoded in the QR code). */
    fun uri(secret: String, account: String, issuer: String): String {
        fun enc(value: String) = java.net.URLEncoder.encode(value, Charsets.UTF_8).replace("+", "%20")
        return "otpauth://totp/${enc(issuer)}:${enc(account)}?secret=$secret&issuer=${enc(issuer)}" +
            "&algorithm=SHA1&digits=$DIGITS&period=$STEP_SECONDS"
    }

    fun base32Encode(bytes: ByteArray): String = buildString {
        var buffer = 0
        var bits = 0
        for (byte in bytes) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xff)
            bits += 8
            while (bits >= 5) {
                append(BASE32[(buffer shr (bits - 5)) and 31])
                bits -= 5
            }
        }
        if (bits > 0) append(BASE32[(buffer shl (5 - bits)) and 31])
    }

    fun base32Decode(text: String): ByteArray {
        val clean = text.uppercase().filter { it != '=' && !it.isWhitespace() }
        val out = java.io.ByteArrayOutputStream()
        var buffer = 0
        var bits = 0
        for (char in clean) {
            val value = BASE32.indexOf(char)
            require(value >= 0) { "Invalid Base32" }
            buffer = (buffer shl 5) or value
            bits += 5
            if (bits >= 8) {
                out.write((buffer shr (bits - 8)) and 0xff)
                bits -= 8
            }
        }
        return out.toByteArray()
    }
}
