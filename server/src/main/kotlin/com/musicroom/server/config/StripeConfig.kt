package com.musicroom.server.config

/**
 * Stripe account selling Music Room Premium. Empty [secretKey] or [priceId]: Premium cannot be bought (billing
 * routes answer 503). [webhookSecret] checks the events Stripe sends; [returnUrl] is the page shown after paying.
 * [automaticTax]: Stripe Tax computes the VAT at checkout (turn on Stripe Tax and add a registration in the
 * Dashboard first).
 */
data class StripeConfig(
    val secretKey: String = "",
    val webhookSecret: String = "",
    val priceId: String = "",
    val returnUrl: String = "http://127.0.0.1:8080/api/v1/billing/return",
    val automaticTax: Boolean = false,
) {
    val configured: Boolean get() = secretKey.isNotEmpty() && priceId.isNotEmpty()
}
