package com.musicroom.server.modules.premium.stripe.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** An event sent by Stripe to the webhook: its [type] and the object it is about. */
@Serializable
data class StripeEvent(val id: String, val type: String, val data: Data) {
    @Serializable
    data class Data(val `object`: JsonObject)
}
