package com.musicroom.server.common.guards

import com.musicroom.server.common.exceptions.UnauthorizedException
import com.musicroom.server.common.jwt.JwtService
import com.musicroom.shared.dto.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import java.util.UUID

private const val JWT_AUTH = "jwt"

fun Application.configureJwtGuard(jwtService: JwtService) {
    authentication {
        jwt(JWT_AUTH) {
            realm = jwtService.realm
            verifier(jwtService.verifier)
            validate { credential ->
                if (credential.payload.subject != null) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("unauthorized", "Missing or invalid token"))
            }
        }
    }
}

fun Route.jwtGuard(build: Route.() -> Unit): Route = authenticate(JWT_AUTH, build = build)

fun ApplicationCall.currentUserId(): UUID {
    val subject = principal<JWTPrincipal>()?.subject ?: throw UnauthorizedException()
    return runCatching { UUID.fromString(subject) }.getOrElse { throw UnauthorizedException() }
}
