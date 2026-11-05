package com.musicroom.server.modules.premium.stripe

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Checks the Stripe-Signature header of a webhook call: "t=<seconds>,v1=<hex HMAC-SHA256 of "t.payload">", signed
 * with the endpoint's secret, and recent enough ([TOLERANCE_SECONDS]) so an old call cannot be replayed.
 */
object StripeSignature {
    const val TOLERANCE_SECONDS = 300L

    fun valid(payload: String, header: String?, secret: String, nowSeconds: Long = System.currentTimeMillis() / 1000): Boolean {
        if (header == null || secret.isEmpty()) return false
        val parts = header.split(',').mapNotNull { part ->
            val index = part.indexOf('=')
            if (index <= 0) null else part.substring(0, index).trim() to part.substring(index + 1).trim()
        }
        val timestamp = parts.firstOrNull { it.first == "t" }?.second?.toLongOrNull() ?: return false
        if (kotlin.math.abs(nowSeconds - timestamp) > TOLERANCE_SECONDS) return false
        val expected = sign("$timestamp.$payload", secret).toByteArray()
        return parts.filter { it.first == "v1" }.any { MessageDigest.isEqual(it.second.toByteArray(), expected) }
    }

    /** The hex signature of [content]; also used by the tests to sign their calls. */
    fun sign(content: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        return mac.doFinal(content.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
