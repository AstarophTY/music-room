package com.musicroom.server.modules.premium.stripe.model

import kotlinx.serialization.Serializable

/**
 * A Stripe subscription. Recent API versions give the period end on each item rather than on the subscription:
 * [periodEnd] reads either (epoch seconds).
 */
@Serializable
data class StripeSubscription(
    val id: String,
    val customer: String,
    val status: String,
    val cancelAtPeriodEnd: Boolean = false,
    val currentPeriodEnd: Long? = null,
    val items: Items? = null,
) {
    val periodEnd: Long? get() = currentPeriodEnd ?: items?.data?.firstNotNullOfOrNull { it.currentPeriodEnd }

    @Serializable
    data class Items(val data: List<Item> = emptyList())

    @Serializable
    data class Item(val currentPeriodEnd: Long? = null)
}
