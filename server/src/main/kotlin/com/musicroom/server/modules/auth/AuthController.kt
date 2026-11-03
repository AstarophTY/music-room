package com.musicroom.server.modules.auth

import com.musicroom.server.common.Controller
import com.musicroom.server.common.guards.currentUserId
import com.musicroom.server.common.guards.jwtGuard
import com.musicroom.server.common.i18n.requestLanguage
import com.musicroom.server.common.throttler.authThrottle
import com.musicroom.shared.ApiRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

class AuthController(
    private val service: AuthService,
    private val emailVerification: EmailVerificationService,
) : Controller {
    override fun Route.routes() {
        authThrottle {
            post(ApiRoutes.AUTH_REGISTER) {
                call.respond(HttpStatusCode.Created, service.register(call.receive(), call.requestLanguage()))
            }
            post(ApiRoutes.AUTH_LOGIN) {
                call.respond(service.login(call.receive()))
            }
            post(ApiRoutes.AUTH_OAUTH) {
                call.respond(service.oauthLogin(call.receive()))
            }
            post(ApiRoutes.AUTH_REFRESH) {
                call.respond(service.refresh(call.receive()))
            }
            post(ApiRoutes.AUTH_LOGOUT) {
                service.logout(call.receive())
                call.respond(HttpStatusCode.NoContent)
            }
            post(ApiRoutes.AUTH_PASSWORD_FORGOT) {
                service.forgotPassword(call.receive(), call.requestLanguage())
                call.respond(HttpStatusCode.NoContent)
            }
            post(ApiRoutes.AUTH_PASSWORD_RESET) {
                call.respond(service.resetPassword(call.receive(), call.requestLanguage()))
            }
            jwtGuard {
                post(ApiRoutes.AUTH_PASSWORD) {
                    call.respond(service.changePassword(call.currentUserId(), call.receive(), call.requestLanguage()))
                }
                post(ApiRoutes.AUTH_EMAIL_VERIFY) {
                    call.respond(emailVerification.verify(call.currentUserId(), call.receive()).toResponse())
                }
                post(ApiRoutes.AUTH_EMAIL_RESEND) {
                    emailVerification.resend(call.currentUserId(), call.requestLanguage())
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
