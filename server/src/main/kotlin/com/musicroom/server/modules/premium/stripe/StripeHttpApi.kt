package com.musicroom.server.modules.premium.stripe

import com.musicroom.server.config.StripeConfig
import com.musicroom.server.modules.premium.stripe.model.StripeCheckoutSession
import com.musicroom.server.modules.premium.stripe.model.StripeId
import com.musicroom.server.modules.premium.stripe.model.StripeSubscription
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

/** Stripe calls over HTTP with the secret key (form-encoded requests, JSON answers). */
class StripeHttpApi(private val config: StripeConfig) : StripeApi {

    @OptIn(ExperimentalSerializationApi::class)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    namingStrategy = JsonNamingStrategy.SnakeCase
                },
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 5_000
        }
    }

    // Asked again for the same user (two taps at once), Stripe gives back the same customer.
    override suspend fun createCustomer(email: String, userId: String): String = post(
        "customers",
        "email" to email,
        "metadata[user_id]" to userId,
        idempotencyKey = "customer-$userId",
    ).body<StripeId>().id

    override suspend fun createCheckoutSession(
        customerId: String,
        priceId: String,
        userId: String,
        successUrl: String,
        cancelUrl: String,
        automaticTax: Boolean,
    ): String {
        // The VAT depends on where the customer lives: the address typed on the page is saved on the customer.
        val tax = if (automaticTax) arrayOf("automatic_tax[enabled]" to "true", "customer_update[address]" to "auto") else emptyArray()
        return post(
            "checkout/sessions",
            "mode" to "subscription",
            "customer" to customerId,
            "client_reference_id" to userId,
            "line_items[0][price]" to priceId,
            "line_items[0][quantity]" to "1",
            "subscription_data[metadata][user_id]" to userId,
            "subscription_data[billing_mode][type]" to "flexible",
            "success_url" to successUrl,
            "cancel_url" to cancelUrl,
            *tax,
        ).body<StripeCheckoutSession>().url ?: throw StripeApiException(0, "Stripe gave no payment page")
    }

    override suspend fun createPortalSession(customerId: String, returnUrl: String): String = post(
        "billing_portal/sessions",
        "customer" to customerId,
        "return_url" to returnUrl,
    ).body<StripeId>().url ?: throw StripeApiException(0, "Stripe gave no portal page")

    override suspend fun checkoutSession(id: String): StripeCheckoutSession = get("checkout/sessions/${id.encodeURLPathPart()}").body()

    override suspend fun subscription(id: String): StripeSubscription = get("subscriptions/${id.encodeURLPathPart()}").body()

    private suspend fun post(path: String, vararg fields: Pair<String, String>, idempotencyKey: String? = null): HttpResponse = call {
        client.submitForm(
            "$BASE_URL/$path",
            Parameters.build { fields.forEach { (name, value) -> append(name, value) } },
        ) {
            authorize()
            idempotencyKey?.let { header("Idempotency-Key", it) }
        }
    }

    private suspend fun get(path: String): HttpResponse = call { client.get("$BASE_URL/$path") { authorize() } }

    /** The secret key, and the API version the answers are read with (a Dashboard upgrade does not change them). */
    private fun HttpRequestBuilder.authorize() {
        bearerAuth(config.secretKey)
        header("Stripe-Version", API_VERSION)
    }

    private suspend fun call(request: suspend () -> HttpResponse): HttpResponse {
        val response = try {
            request()
        } catch (e: Exception) {
            throw StripeApiException(0, e.message ?: "Stripe could not be reached")
        }
        if (!response.status.isSuccess()) throw StripeApiException(response.status.value, response.bodyAsText().take(500))
        return response
    }

    private companion object {
        const val BASE_URL = "https://api.stripe.com/v1"
        const val API_VERSION = "2025-09-30.clover"
    }
}
