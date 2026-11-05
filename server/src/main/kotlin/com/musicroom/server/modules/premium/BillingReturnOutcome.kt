package com.musicroom.server.modules.premium

/** How the Stripe payment page ended, for the page shown after it. */
enum class BillingReturnOutcome { Paid, Pending, Cancelled }
