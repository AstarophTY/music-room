package com.musicroom.server.common.filters

import com.musicroom.server.common.exceptions.HttpException
import com.musicroom.shared.dto.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureExceptionFilter() {
    install(StatusPages) {
        exception<HttpException> { call, cause ->
            call.respond(cause.status, ErrorResponse(cause.error, cause.message))
        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("bad_request", "Invalid request body"))
        }
        exception<Throwable> { call, cause ->
            this@configureExceptionFilter.log.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("internal_error"))
        }
        status(HttpStatusCode.TooManyRequests) { call, status ->
            call.respond(status, ErrorResponse("too_many_requests"))
        }
    }
}
