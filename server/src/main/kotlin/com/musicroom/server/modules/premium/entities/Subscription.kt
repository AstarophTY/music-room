package com.musicroom.server.modules.premium.entities

import java.time.OffsetDateTime
import java.util.UUID

/** A user's Stripe customer and their Premium subscription, as Stripe last reported it. */
data class Subscription(
    val userId: UUID,
    val customerId: String,
    val subscriptionId: String?,
    val status: String,
    val currentPeriodEnd: OffsetDateTime?,
    val cancelAtPeriodEnd: Boolean,
    val updatedAt: OffsetDateTime,
) {
    /** Paid (or in a trial): the user has Premium. */
    val premium: Boolean get() = status in PREMIUM_STATUSES

    companion object {
        val PREMIUM_STATUSES = setOf("active", "trialing")
    }
}
