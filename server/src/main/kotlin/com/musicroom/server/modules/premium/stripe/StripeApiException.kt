package com.musicroom.server.modules.premium.stripe

/** Stripe answered [status] (0: not reached). */
class StripeApiException(val status: Int, message: String) : RuntimeException(message)
