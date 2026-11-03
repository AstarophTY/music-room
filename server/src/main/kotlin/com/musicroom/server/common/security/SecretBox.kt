package com.musicroom.server.common.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypts small secrets before they go to the database (AES-256-GCM), so a database leak alone does not
 * reveal them. The key is derived from [masterSecret] (JWT_SECRET), with a label so it differs from the JWT key.
 */
class SecretBox(masterSecret: String) {
    private val key = SecretKeySpec(
        MessageDigest.getInstance("SHA-256").digest("music-room/secret-box/v1:$masterSecret".toByteArray()),
        "AES",
    )
    private val random = SecureRandom()

    fun encrypt(plain: String): String {
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv)) }
        return Base64.getEncoder().encodeToString(iv + cipher.doFinal(plain.toByteArray()))
    }

    fun decrypt(encrypted: String): String {
        val bytes = Base64.getDecoder().decode(encrypted)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
        }
        return String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES))
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
