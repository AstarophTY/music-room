package com.musicroom.server.modules.social

import com.musicroom.server.common.Controller
import com.musicroom.server.common.exceptions.NotFoundException
import com.musicroom.server.common.guards.currentUserId
import com.musicroom.server.common.guards.jwtGuard
import com.musicroom.server.common.jwt.JwtService
import com.musicroom.shared.ApiRoutes
import com.musicroom.shared.MusicRoomJson
import com.musicroom.shared.dto.SendMessageRequest
import com.musicroom.shared.dto.SocialClientEvent
import com.musicroom.shared.dto.SocialEvent
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import java.util.UUID
import kotlinx.serialization.SerializationException

class SocialController(private val service: SocialService, private val hub: SocialHub, private val jwt: JwtService) : Controller {
    override fun Route.routes() {
        jwtGuard {
            get(ApiRoutes.USERS_SEARCH) {
                call.respond(service.search(call.currentUserId(), call.request.queryParameters["q"].orEmpty()))
            }
            get(ApiRoutes.USER_PROFILE) {
                call.respond(service.profile(call.currentUserId(), call.uuid("id")))
            }

            get(ApiRoutes.FRIENDS) {
                call.respond(service.friends(call.currentUserId()))
            }
            post(ApiRoutes.FRIEND) {
                call.respond(service.addFriend(call.currentUserId(), call.uuid("userId")))
            }
            delete(ApiRoutes.FRIEND) {
                service.removeFriend(call.currentUserId(), call.uuid("userId"))
                call.respond(HttpStatusCode.NoContent)
            }

            get(ApiRoutes.CONVERSATIONS) {
                call.respond(service.conversations(call.currentUserId()))
            }
            get(ApiRoutes.CONVERSATION_MESSAGES) {
                val before = call.request.queryParameters["before"]?.toLongOrNull()
                call.respond(service.messages(call.currentUserId(), call.uuid("userId"), before))
            }
            post(ApiRoutes.CONVERSATION_MESSAGES) {
                val request = call.receive<SendMessageRequest>()
                call.respond(HttpStatusCode.Created, service.send(call.currentUserId(), call.uuid("userId"), request.body))
            }
            post(ApiRoutes.CONVERSATION_READ) {
                service.markRead(call.currentUserId(), call.uuid("userId"))
                call.respond(HttpStatusCode.NoContent)
            }

            get(ApiRoutes.NOTIFICATIONS) {
                call.respond(service.notifications(call.currentUserId()))
            }
            post(ApiRoutes.NOTIFICATIONS_READ) {
                service.markNotificationsRead(call.currentUserId())
                call.respond(HttpStatusCode.NoContent)
            }
            get(ApiRoutes.SOCIAL_COUNTS) {
                call.respond(service.counts(call.currentUserId()))
            }
        }

        // Browsers cannot set headers on a WebSocket: the access token comes in the "token" parameter.
        webSocket(ApiRoutes.SOCIAL_WS) {
            val userId = call.request.queryParameters["token"]?.let { token ->
                runCatching { UUID.fromString(jwt.verifier.verify(token).subject) }.getOrNull()
            }
            if (userId == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "unauthorized"))
                return@webSocket
            }
            hub.join(userId, this)
            try {
                send(Frame.Text(MusicRoomJson.encodeToString(SocialEvent.serializer(), SocialEvent.Counts(service.counts(userId)))))
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    val event = try {
                        MusicRoomJson.decodeFromString(SocialClientEvent.serializer(), frame.readText())
                    } catch (e: SerializationException) {
                        continue
                    } catch (e: IllegalArgumentException) {
                        continue
                    }
                    when (event) {
                        is SocialClientEvent.Typing ->
                            runCatching { UUID.fromString(event.userId) }.getOrNull()?.let { service.typing(userId, it, event.typing) }
                    }
                }
            } finally {
                hub.leave(userId, this)
            }
        }
    }

    private fun ApplicationCall.uuid(name: String): UUID =
        parameters[name]?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: throw NotFoundException("No such user")
}
