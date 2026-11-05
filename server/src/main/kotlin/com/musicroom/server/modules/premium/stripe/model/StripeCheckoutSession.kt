package com.musicroom.server.modules.premium.stripe.model

import kotlinx.serialization.Serializable

/** A Stripe payment page: [subscription] is set once the user paid; [clientReferenceId] is the Music Room user id. */
@Serializable
data class StripeCheckoutSession(
    val id: String,
    val customer: String? = null,
    val subscription: String? = null,
    val clientReferenceId: String? = null,
    val url: String? = null,
)
