package com.musicroom.server.modules.social

import com.musicroom.shared.MusicRoomJson
import com.musicroom.shared.dto.SocialEvent
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import org.slf4j.LoggerFactory

/** Open social WebSockets of each user (one per device), to push them live events. */
class SocialHub(private val relay: SocialRelay? = null) {
    private val log = LoggerFactory.getLogger(SocialHub::class.java)
    private val sessions = ConcurrentHashMap<UUID, MutableSet<WebSocketSession>>()

    fun join(userId: UUID, session: WebSocketSession) {
        sessions.computeIfAbsent(userId) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    fun leave(userId: UUID, session: WebSocketSession) {
        sessions.computeIfPresent(userId) { _, set -> set.apply { remove(session) }.ifEmpty { null } }
    }

    /** Sends [event] to every device of [userId] that is connected; the others get it from the API later. */
    suspend fun send(userId: UUID, event: SocialEvent) {
        if (relay == null && sessions[userId].isNullOrEmpty()) return
        val text = MusicRoomJson.encodeToString(SocialEvent.serializer(), event)
        if (relay != null) relay.publish(userId, text) else deliver(userId, text)
    }

    /** Writes an encoded event to the WebSockets of [userId] open on this process. */
    suspend fun deliver(userId: UUID, text: String) {
        sessions[userId]?.toList().orEmpty().forEach { session ->
            try {
                session.send(Frame.Text(text))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The socket is closing: its own loop removes it.
                log.debug("Could not push a social event: {}", e.message)
            }
        }
    }
}
