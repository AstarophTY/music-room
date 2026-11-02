package com.musicroom.server.common.interceptors

import com.musicroom.shared.ApiRoutes
import com.musicroom.shared.ClientHeaders
import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import org.slf4j.event.Level
import java.util.UUID

fun Application.configureLoggingInterceptor() {
    install(CallId) {
        retrieveFromHeader(HttpHeaders.XRequestId)
        generate { UUID.randomUUID().toString() }
        replyToHeader(HttpHeaders.XRequestId)
    }
    install(CallLogging) {
        level = Level.INFO
        callIdMdc("call-id")
        mdc("platform") { it.request.headers[ClientHeaders.PLATFORM] ?: "-" }
        mdc("device") { it.request.headers[ClientHeaders.DEVICE] ?: "-" }
        mdc("appVersion") { it.request.headers[ClientHeaders.APP_VERSION] ?: "-" }
        filter { it.request.path() != ApiRoutes.HEALTH }
        format { call ->
            "${call.response.status()?.value} ${call.request.httpMethod.value} ${call.request.path()}"
        }
    }
}
