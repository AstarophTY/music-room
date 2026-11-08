package com.musicroom.server.modules.events

import com.musicroom.server.common.Controller
import com.musicroom.server.common.exceptions.HttpException
import com.musicroom.server.common.exceptions.NotFoundException
import com.musicroom.server.common.guards.currentUserId
import com.musicroom.server.common.guards.jwtGuard
import com.musicroom.server.common.jwt.JwtService
import com.musicroom.shared.ApiRoutes
import com.musicroom.shared.dto.CreateEventRequest
import com.musicroom.shared.dto.EventVoteRequest
import com.musicroom.shared.dto.EventsScope
import com.musicroom.shared.dto.GeoPoint
import com.musicroom.shared.dto.ProposeTrackRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import java.util.UUID

class EventsController(private val service: EventsService, private val jwt: JwtService) : Controller {
    override fun Route.routes() {
        jwtGuard {
            // ?scope=public|nearby|invited|mine; nearby needs lat and lng.
            get(ApiRoutes.EVENTS) {
                val scope = call.request.queryParameters["scope"]?.let { value ->
                    EventsScope.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                } ?: EventsScope.PUBLIC
                val lat = call.request.queryParameters["lat"]?.toDoubleOrNull()
                val lng = call.request.queryParameters["lng"]?.toDoubleOrNull()
                val near = if (lat != null && lng != null) GeoPoint(lat, lng) else null
                call.respond(service.list(call.currentUserId(), scope, near))
            }
            post(ApiRoutes.EVENTS) {
                call.respond(HttpStatusCode.Created, service.create(call.currentUserId(), call.receive<CreateEventRequest>()))
            }
            get(ApiRoutes.EVENT) {
                call.respond(service.detail(call.currentUserId(), call.uuid("id")))
            }
            put(ApiRoutes.EVENT) {
                call.respond(service.update(call.currentUserId(), call.uuid("id"), call.receive<CreateEventRequest>()))
            }
            delete(ApiRoutes.EVENT) {
                service.delete(call.currentUserId(), call.uuid("id"))
                call.respond(HttpStatusCode.NoContent)
            }

            post(ApiRoutes.EVENT_TRACKS) {
                val request = call.receive<ProposeTrackRequest>()
                call.respond(service.propose(call.currentUserId(), call.uuid("id"), request.trackId, request.location, request.catalogId))
            }
            delete(ApiRoutes.EVENT_TRACK) {
                call.respond(service.removeTrack(call.currentUserId(), call.uuid("id"), call.uuid("trackId")))
            }
            post(ApiRoutes.EVENT_TRACK_VOTE) {
                val request = call.receiveNullable<EventVoteRequest>() ?: EventVoteRequest()
                call.respond(service.vote(call.currentUserId(), call.uuid("id"), call.uuid("trackId"), request.location))
            }
            delete(ApiRoutes.EVENT_TRACK_VOTE) {
                call.respond(service.unvote(call.currentUserId(), call.uuid("id"), call.uuid("trackId")))
            }

            post(ApiRoutes.EVENT_INVITED) {
                call.respond(service.invite(call.currentUserId(), call.uuid("id"), call.uuid("userId")))
            }
            delete(ApiRoutes.EVENT_INVITED) {
                service.uninvite(call.currentUserId(), call.uuid("id"), call.uuid("userId"))
                call.respond(HttpStatusCode.NoContent)
            }

            post(ApiRoutes.EVENT_PLAY) { call.respond(service.play(call.currentUserId(), call.uuid("id"))) }
            post(ApiRoutes.EVENT_PAUSE) { call.respond(service.pause(call.currentUserId(), call.uuid("id"))) }
            post(ApiRoutes.EVENT_SKIP) { call.respond(service.skip(call.currentUserId(), call.uuid("id"))) }
            post(ApiRoutes.EVENT_STOP) { call.respond(service.stop(call.currentUserId(), call.uuid("id"))) }
        }

        // Browsers cannot set headers on a WebSocket: the access token comes in the "token" parameter.
        webSocket(ApiRoutes.EVENT_WS) {
            val userId = call.request.queryParameters["token"]?.let { token ->
                runCatching { UUID.fromString(jwt.verifier.verify(token).subject) }.getOrNull()
            }
            val eventId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            if (userId == null || eventId == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "unauthorized"))
                return@webSocket
            }
            try {
                service.visible(userId, eventId)
            } catch (e: HttpException) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, e.error))
                return@webSocket
            }
            // Joining pushes the current state; then every change. Nothing is expected from the app.
            service.hub.join(eventId, userId, this)
            try {
                for (frame in incoming) Unit
            } finally {
                service.hub.leave(eventId, userId, this)
            }
        }
    }

    private fun ApplicationCall.uuid(name: String): UUID =
        parameters[name]?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: throw NotFoundException("Not found")
}
