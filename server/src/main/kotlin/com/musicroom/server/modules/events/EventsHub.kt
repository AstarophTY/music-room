package com.musicroom.server.modules.events

import com.musicroom.shared.MusicRoomJson
import com.musicroom.shared.dto.EventDetailResponse
import com.musicroom.shared.dto.EventLiveEvent
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

/**
 * People following each event on a WebSocket. A change is pushed to all of them, each with the state as they see
 * it ([render]). Changes close together (a burst of votes) are sent once, [COALESCE_MS] after the first.
 */
class EventsHub(private val render: suspend (eventId: UUID, userIds: Set<UUID>) -> Map<UUID, EventDetailResponse>) {
    private val log = LoggerFactory.getLogger(EventsHub::class.java)
    private val sessions = ConcurrentHashMap<UUID, MutableSet<Pair<UUID, WebSocketSession>>>()
    private val scheduled: MutableSet<UUID> = ConcurrentHashMap.newKeySet()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun join(eventId: UUID, userId: UUID, session: WebSocketSession) {
        sessions.computeIfAbsent(eventId) { ConcurrentHashMap.newKeySet() }.add(userId to session)
        changed(eventId)
    }

    fun leave(eventId: UUID, userId: UUID, session: WebSocketSession) {
        sessions.computeIfPresent(eventId) { _, set -> set.apply { remove(userId to session) }.ifEmpty { null } }
        changed(eventId)
    }

    /** Distinct people following [eventId]. */
    fun listeners(eventId: UUID): Int = sessions[eventId]?.map { it.first }?.toSet()?.size ?: 0

    /** The event changed: its followers get the new state shortly. */
    fun changed(eventId: UUID) {
        if (!scheduled.add(eventId)) return
        scope.launch {
            delay(COALESCE_MS)
            scheduled.remove(eventId)
            push(eventId)
        }
    }

    /** The event was deleted: its followers are told, then disconnected by their own loop. */
    suspend fun deleted(eventId: UUID) {
        val targets = sessions.remove(eventId).orEmpty()
        val text = MusicRoomJson.encodeToString(EventLiveEvent.serializer(), EventLiveEvent.Deleted)
        targets.forEach { (_, session) -> sendQuietly(session, text) }
    }

    private suspend fun push(eventId: UUID) {
        val targets = sessions[eventId]?.toList().orEmpty()
        if (targets.isEmpty()) return
        val states = try {
            render(eventId, targets.map { it.first }.toSet())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn("Could not render event {} for its followers: {}", eventId, e.message)
            return
        }
        targets.forEach { (userId, session) ->
            val state = states[userId] ?: return@forEach
            sendQuietly(session, MusicRoomJson.encodeToString(EventLiveEvent.serializer(), EventLiveEvent.State(state)))
        }
    }

    private suspend fun sendQuietly(session: WebSocketSession, text: String) {
        try {
            session.send(Frame.Text(text))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The socket is closing: its own loop removes it.
            log.debug("Could not push an event update: {}", e.message)
        }
    }

    private companion object {
        const val COALESCE_MS = 120L
    }
}
