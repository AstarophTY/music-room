package com.musicroom.server.modules.premium.stripe.model

import kotlinx.serialization.Serializable

/** A Stripe object of which only the [id] (a customer) or the [url] (a portal page) is needed. */
@Serializable
data class StripeId(val id: String = "", val url: String? = null)
