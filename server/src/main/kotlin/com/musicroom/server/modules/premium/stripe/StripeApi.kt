package com.musicroom.server.modules.premium.stripe

import com.musicroom.server.modules.premium.stripe.model.StripeCheckoutSession
import com.musicroom.server.modules.premium.stripe.model.StripeSubscription

/** The Stripe calls Music Room Premium needs. Throws [StripeApiException] when Stripe refuses or cannot be reached. */
interface StripeApi {
    /** Creates the Stripe customer of a user; returns its id. */
    suspend fun createCustomer(email: String, userId: String): String

    /** Creates the payment page of a monthly subscription to [priceId]; returns its URL. */
    suspend fun createCheckoutSession(customerId: String, priceId: String, userId: String, successUrl: String, cancelUrl: String): String

    /** Creates the page where the customer manages or cancels their subscription; returns its URL. */
    suspend fun createPortalSession(customerId: String, returnUrl: String): String

    suspend fun checkoutSession(id: String): StripeCheckoutSession

    suspend fun subscription(id: String): StripeSubscription
}
