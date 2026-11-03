package com.musicroom.server.common.security

import java.security.MessageDigest

/** Hash for secrets we only need to compare (refresh tokens, email codes), never to read back. */
object Sha256 {
    fun hex(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    /** Constant-time comparison, so response time does not leak how many characters matched. */
    fun matches(value: String, expectedHex: String): Boolean =
        MessageDigest.isEqual(hex(value).toByteArray(), expectedHex.toByteArray())
}
