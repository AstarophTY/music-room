package com.musicroom.server.modules.premium

import com.musicroom.server.modules.premium.stripe.StripeApi
import com.musicroom.server.modules.premium.stripe.StripeApiException
import com.musicroom.server.modules.premium.stripe.model.StripeCheckoutSession
import com.musicroom.server.modules.premium.stripe.model.StripeSubscription
import java.util.UUID

/** Stripe in memory: a payment page is paid with [pay]; subscriptions can be changed in [subscriptions]. */
class FakeStripeApi : StripeApi {
    val customers = mutableMapOf<String, String>()
    val sessions = mutableMapOf<String, StripeCheckoutSession>()
    val subscriptions = mutableMapOf<String, StripeSubscription>()
    var lastSuccessUrl: String? = null

    override suspend fun createCustomer(email: String, userId: String): String {
        val id = "cus_${UUID.randomUUID()}"
        customers[id] = email
        return id
    }

    override suspend fun createCheckoutSession(
        customerId: String,
        priceId: String,
        userId: String,
        successUrl: String,
        cancelUrl: String,
    ): String {
        val id = "cs_${sessions.size + 1}"
        sessions[id] = StripeCheckoutSession(id = id, customer = customerId, clientReferenceId = userId)
        lastSuccessUrl = successUrl
        return "https://checkout.stripe.test/$id"
    }

    override suspend fun createPortalSession(customerId: String, returnUrl: String): String = "https://billing.stripe.test/$customerId"

    override suspend fun checkoutSession(id: String): StripeCheckoutSession = sessions[id] ?: throw StripeApiException(404, "No such session")

    override suspend fun subscription(id: String): StripeSubscription = subscriptions[id] ?: throw StripeApiException(404, "No such subscription")

    /** The user pays on the page [sessionId]: an active subscription is created. */
    fun pay(sessionId: String): StripeSubscription {
        val session = sessions.getValue(sessionId)
        val subscription = StripeSubscription(
            id = "sub_${UUID.randomUUID()}",
            customer = session.customer.orEmpty(),
            status = "active",
            items = StripeSubscription.Items(listOf(StripeSubscription.Item(currentPeriodEnd = 4_102_444_800))),
        )
        subscriptions[subscription.id] = subscription
        sessions[sessionId] = session.copy(subscription = subscription.id)
        return subscription
    }
}
