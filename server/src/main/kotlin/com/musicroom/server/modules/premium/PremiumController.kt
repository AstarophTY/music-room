package com.musicroom.server.modules.premium

import com.musicroom.server.common.Controller
import com.musicroom.server.common.exceptions.NotFoundException
import com.musicroom.server.common.guards.currentUserId
import com.musicroom.server.common.guards.jwtGuard
import com.musicroom.server.common.i18n.requestLanguage
import com.musicroom.shared.ApiRoutes
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.contentLength
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.receiveText
import io.ktor.server.response.cacheControl
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import java.util.UUID

/** Music Room Premium (Stripe) and the songs Premium users upload. */
class PremiumController(private val billing: BillingService, private val tracks: HostedTracksService) : Controller {
    override fun Route.routes() {
        // Called by the browser after the Stripe pages, and by Stripe: no user token.
        get(ApiRoutes.BILLING_RETURN) {
            val outcome = billing.completeCheckout(call.request.queryParameters["session_id"])
            call.respondText(BillingReturnPage.html(outcome, call.requestLanguage()), ContentType.Text.Html)
        }
        post(ApiRoutes.BILLING_WEBHOOK) {
            billing.webhook(call.receiveText(), call.request.headers["Stripe-Signature"])
            call.respond(HttpStatusCode.OK, mapOf("received" to true))
        }
        jwtGuard {
            get(ApiRoutes.BILLING) {
                call.respond(billing.status(call.currentUserId()))
            }
            post(ApiRoutes.BILLING_CHECKOUT) {
                call.respond(billing.checkout(call.currentUserId()))
            }
            post(ApiRoutes.BILLING_PORTAL) {
                call.respond(billing.portal(call.currentUserId()))
            }
            get(ApiRoutes.HOSTED_TRACKS) {
                call.respond(tracks.mine(call.currentUserId()))
            }
            post(ApiRoutes.HOSTED_TRACKS) {
                val declared = call.request.contentLength()
                if (declared != null && declared > HostedTracksService.MAX_UPLOAD_BYTES) throw HostedTracksService.tooLarge()
                // Read one byte more than allowed: a bigger body without Content-Length is refused too.
                val audio = call.receiveChannel().readRemaining(HostedTracksService.MAX_UPLOAD_BYTES + 1L).readByteArray()
                val params = call.request.queryParameters
                call.respond(HttpStatusCode.Created, tracks.upload(call.currentUserId(), audio, params["title"], params["artist"]))
            }
            delete(ApiRoutes.HOSTED_TRACK) {
                tracks.delete(call.currentUserId(), call.uuid("id"))
                call.respond(HttpStatusCode.NoContent)
            }
            get(ApiRoutes.HOSTED_TRACK_AUDIO) {
                val audio = tracks.audio(call.uuid("id"))
                // A song never changes: the app can keep it.
                call.response.cacheControl(CacheControl.MaxAge(maxAgeSeconds = 86_400, visibility = CacheControl.Visibility.Private))
                call.respondBytes(audio, ContentType.Audio.MPEG)
            }
            get(ApiRoutes.USER_TRACKS) {
                call.respond(tracks.byUser(call.uuid("id")))
            }
        }
    }

    private fun ApplicationCall.uuid(name: String): UUID =
        parameters[name]?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: throw NotFoundException("Not found")
}
