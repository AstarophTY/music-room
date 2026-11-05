package com.musicroom.server.modules.premium

import com.musicroom.server.common.exceptions.BadRequestException
import com.musicroom.server.common.exceptions.ConflictException
import com.musicroom.server.common.exceptions.HttpException
import com.musicroom.server.common.exceptions.NotFoundException
import com.musicroom.server.config.StripeConfig
import com.musicroom.server.modules.premium.entities.Subscription
import com.musicroom.server.modules.premium.stripe.StripeApi
import com.musicroom.server.modules.premium.stripe.StripeApiException
import com.musicroom.server.modules.premium.stripe.StripeSignature
import com.musicroom.server.modules.premium.stripe.model.StripeEvent
import com.musicroom.server.modules.premium.stripe.model.StripeSubscription
import com.musicroom.server.modules.users.UsersRepository
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.dto.BillingUrlResponse
import com.musicroom.shared.dto.SubscriptionResponse
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * Music Room Premium, a monthly subscription paid on Stripe's pages. Stripe tells the server about payments and
 * cancellations through the webhook; without one (development), the status is read from Stripe after paying and
 * again every few minutes ([REFRESH_AFTER_SECONDS]).
 */
class BillingService(
    private val config: StripeConfig,
    private val api: StripeApi,
    private val subscriptions: SubscriptionsRepository,
    private val users: UsersRepository,
) {
    private val log = LoggerFactory.getLogger(BillingService::class.java)

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

    suspend fun status(userId: UUID): SubscriptionResponse {
        if (!config.configured) return SubscriptionResponse(configured = false, premium = false)
        var subscription = subscriptions.find(userId)
        val subscriptionId = subscription?.subscriptionId
        if (subscription != null && subscriptionId != null && subscription.updatedAt.isBefore(OffsetDateTime.now().minusSeconds(REFRESH_AFTER_SECONDS))) {
            subscription = try {
                save(api.subscription(subscriptionId))
                subscriptions.find(userId)
            } catch (e: StripeApiException) {
                log.warn("Stripe subscription not refreshed: {}", e.message)
                subscription
            }
        }
        return subscription.toResponse()
    }

    /** The user has Premium right now. */
    suspend fun isPremium(userId: UUID): Boolean = config.configured && status(userId).premium

    /** The Stripe page to pay for Premium. */
    suspend fun checkout(userId: UUID): BillingUrlResponse {
        requireConfigured()
        val existing = subscriptions.find(userId)
        if (existing?.premium == true) throw ConflictException("You already have Premium", ApiErrors.ALREADY_PREMIUM)
        val subscription = existing ?: run {
            val user = users.findById(userId) ?: throw NotFoundException("User not found")
            subscriptions.createCustomer(userId, stripe { api.createCustomer(user.email, userId.toString()) })
        }
        val url = stripe {
            api.createCheckoutSession(
                customerId = subscription.customerId,
                priceId = config.priceId,
                userId = userId.toString(),
                // Stripe puts the id of the payment page in place of {CHECKOUT_SESSION_ID}.
                successUrl = returnUrl("session_id={CHECKOUT_SESSION_ID}"),
                cancelUrl = returnUrl("cancelled=true"),
                automaticTax = config.automaticTax,
            )
        }
        return BillingUrlResponse(url)
    }

    /** The Stripe page to manage or cancel the subscription. */
    suspend fun portal(userId: UUID): BillingUrlResponse {
        requireConfigured()
        val subscription = subscriptions.find(userId)
            ?: throw BadRequestException("Subscribe to Premium first", ApiErrors.PREMIUM_REQUIRED)
        return BillingUrlResponse(stripe { api.createPortalSession(subscription.customerId, config.returnUrl) })
    }

    /** The user came back from the payment page: reads the new subscription from Stripe ([sessionId] null: cancelled). */
    suspend fun completeCheckout(sessionId: String?): BillingReturnOutcome {
        if (sessionId.isNullOrBlank() || !config.configured) return BillingReturnOutcome.Cancelled
        return try {
            val subscriptionId = api.checkoutSession(sessionId).subscription ?: return BillingReturnOutcome.Pending
            val subscription = api.subscription(subscriptionId)
            save(subscription)
            if (subscription.status in Subscription.PREMIUM_STATUSES) BillingReturnOutcome.Paid else BillingReturnOutcome.Pending
        } catch (e: StripeApiException) {
            log.warn("Stripe payment page not read: {}", e.message)
            BillingReturnOutcome.Pending
        }
    }

    /** An event from Stripe: payments, renewals and cancellations update the subscription. Refused unless signed. */
    suspend fun webhook(payload: String, signature: String?) {
        if (config.webhookSecret.isEmpty()) throw NotFoundException("No webhook")
        if (!StripeSignature.valid(payload, signature, config.webhookSecret)) throw BadRequestException("Invalid signature")
        val event = try {
            json.decodeFromString(StripeEvent.serializer(), payload)
        } catch (e: SerializationException) {
            throw BadRequestException("Invalid event")
        }
        val subject = event.data.`object`
        when (event.type) {
            "checkout.session.completed" -> {
                val subscriptionId = subject["subscription"]?.jsonPrimitive?.contentOrNull ?: return
                save(stripe { api.subscription(subscriptionId) })
            }
            // Stripe may send events out of order: the subscription is read again as it is now.
            "customer.subscription.created", "customer.subscription.updated", "customer.subscription.deleted" -> {
                val subscriptionId = subject["id"]?.jsonPrimitive?.contentOrNull ?: return
                save(stripe { api.subscription(subscriptionId) })
            }
        }
    }

    /** Saves [subscription]; an older one ending does not take Premium away from a newer one still paid. */
    private suspend fun save(subscription: StripeSubscription) {
        val current = subscriptions.findByCustomer(subscription.customer) ?: return
        val premium = subscription.status in Subscription.PREMIUM_STATUSES
        if (current.premium && current.subscriptionId != null && current.subscriptionId != subscription.id && !premium) return
        subscriptions.update(
            customerId = subscription.customer,
            subscriptionId = subscription.id,
            status = subscription.status,
            periodEnd = subscription.periodEnd?.let { OffsetDateTime.ofInstant(Instant.ofEpochSecond(it), ZoneOffset.UTC) },
            cancelAtPeriodEnd = subscription.cancelAtPeriodEnd,
        )
    }

    private fun Subscription?.toResponse() = SubscriptionResponse(
        configured = true,
        premium = this?.premium == true,
        status = this?.status?.takeIf { it != SubscriptionsRepository.NO_SUBSCRIPTION },
        renewsAt = this?.currentPeriodEnd?.toInstant()?.toEpochMilli(),
        cancelAtPeriodEnd = this?.cancelAtPeriodEnd == true,
    )

    private fun returnUrl(query: String): String {
        val separator = if ('?' in config.returnUrl) '&' else '?'
        return "${config.returnUrl}$separator$query"
    }

    private fun requireConfigured() {
        if (!config.configured) {
            throw HttpException(HttpStatusCode.ServiceUnavailable, ApiErrors.BILLING_NOT_CONFIGURED, "Premium is not available on this server")
        }
    }

    private suspend fun <T> stripe(call: suspend () -> T): T = try {
        call()
    } catch (e: StripeApiException) {
        log.warn("Stripe error: {} {}", e.status, e.message)
        throw HttpException(HttpStatusCode.BadGateway, ApiErrors.BILLING_UNAVAILABLE, "Stripe did not answer, try again later")
    }

    private companion object {
        const val REFRESH_AFTER_SECONDS = 300L
    }
}
