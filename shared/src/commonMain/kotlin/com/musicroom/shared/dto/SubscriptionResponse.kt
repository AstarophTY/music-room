package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/**
 * The user's Music Room Premium subscription. [configured] is false when the server has no Stripe account (no
 * subscribing then). [renewsAt] (epoch ms) is when it renews, or ends when [cancelAtPeriodEnd].
 */
@Serializable
data class SubscriptionResponse(
    val configured: Boolean,
    val premium: Boolean,
    val status: String? = null,
    val renewsAt: Long? = null,
    val cancelAtPeriodEnd: Boolean = false,
)
