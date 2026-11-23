package com.musicroom.server.common.throttler

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.routing.Route
import kotlin.time.Duration.Companion.minutes

private val AUTH_THROTTLE = RateLimitName("auth")

fun Application.configureThrottler(trustProxy: Boolean = false) {
    fun clientIp(call: ApplicationCall): String =
        call.request.headers["X-Real-Ip"].takeIf { trustProxy && !it.isNullOrBlank() } ?: call.request.origin.remoteHost

    install(RateLimit) {
        global {
            rateLimiter(limit = 300, refillPeriod = 1.minutes)
            requestKey { clientIp(it) }
        }
        register(AUTH_THROTTLE) {
            rateLimiter(limit = 10, refillPeriod = 1.minutes)
            requestKey { clientIp(it) }
        }
    }
}

/** Stricter limit (10 requests/min per IP) for login, sign-up and other brute-forceable routes. */
fun Route.authThrottle(build: Route.() -> Unit): Route = rateLimit(AUTH_THROTTLE, build)
