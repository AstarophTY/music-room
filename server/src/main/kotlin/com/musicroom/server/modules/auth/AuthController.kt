package com.musicroom.server.modules.auth

import com.musicroom.server.common.Controller
import com.musicroom.server.common.guards.currentUserId
import com.musicroom.server.common.guards.jwtGuard
import com.musicroom.server.common.i18n.requestLanguage
import com.musicroom.server.common.throttler.authThrottle
import com.musicroom.shared.ApiRoutes
import com.musicroom.shared.dto.TwoFactorCodeRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

class AuthController(
    private val service: AuthService,
    private val emailVerification: EmailVerificationService,
    private val twoFactor: TwoFactorService,
) : Controller {
    override fun Route.routes() {
        authThrottle {
            post(ApiRoutes.AUTH_REGISTER) {
                call.respond(HttpStatusCode.Created, service.register(call.receive(), call.requestLanguage()))
            }
            post(ApiRoutes.AUTH_LOGIN) {
                call.respondLogin(service.login(call.receive()))
            }
            post(ApiRoutes.AUTH_OAUTH) {
                call.respondLogin(service.oauthLogin(call.receive()))
            }
            post(ApiRoutes.AUTH_2FA_VERIFY) {
                call.respond(service.verifyTwoFactor(call.receive()))
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
                call.respondLogin(service.resetPassword(call.receive(), call.requestLanguage()))
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
                post(ApiRoutes.AUTH_2FA_SETUP) {
                    call.respond(twoFactor.setup(call.currentUserId()))
                }
                post(ApiRoutes.AUTH_2FA_ENABLE) {
                    val request = call.receive<TwoFactorCodeRequest>()
                    call.respond(twoFactor.enable(call.currentUserId(), request.code, call.requestLanguage()))
                }
                post(ApiRoutes.AUTH_2FA_DISABLE) {
                    val request = call.receive<TwoFactorCodeRequest>()
                    call.respond(twoFactor.disable(call.currentUserId(), request.code, call.requestLanguage()).toResponse())
                }
            }
        }
    }

    /** 200 with tokens, or 202 with a challenge when the account has 2FA. */
    private suspend fun ApplicationCall.respondLogin(result: LoginResult) = when (result) {
        is LoginResult.Success -> respond(result.auth)
        is LoginResult.TwoFactorRequired -> respond(HttpStatusCode.Accepted, result.challenge)
    }
}
