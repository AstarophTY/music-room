package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** A Stripe page to open in the browser: the payment page, or the page to manage the subscription. */
@Serializable
data class BillingUrlResponse(val url: String)
