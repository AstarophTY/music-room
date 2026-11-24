package com.musicroom.server.modules.playlists

import com.musicroom.shared.MusicRoomJson
import com.musicroom.shared.dto.PlaylistDetailResponse
import com.musicroom.shared.dto.PlaylistLiveEvent
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
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
 * People with a playlist open on a WebSocket. A change is pushed to each of them as they see it ([render]); someone
 * who lost access is told and disconnected. Changes close together are sent once, [COALESCE_MS] after the first.
 * With [publish], a change first goes through PostgreSQL so that every replica pushes to its own sockets.
 */
class PlaylistsHub(
    private val render: suspend (playlistId: UUID, userIds: Set<UUID>) -> Map<UUID, PlaylistDetailResponse>,
    private val publish: (suspend (UUID) -> Unit)? = null,
) {
    private val log = LoggerFactory.getLogger(PlaylistsHub::class.java)
    private val sessions = ConcurrentHashMap<UUID, MutableSet<Pair<UUID, WebSocketSession>>>()
    private val scheduled: MutableSet<UUID> = ConcurrentHashMap.newKeySet()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun join(playlistId: UUID, userId: UUID, session: WebSocketSession) {
        sessions.computeIfAbsent(playlistId) { ConcurrentHashMap.newKeySet() }.add(userId to session)
        schedule(playlistId)
    }

    fun leave(playlistId: UUID, userId: UUID, session: WebSocketSession) {
        sessions.computeIfPresent(playlistId) { _, set -> set.apply { remove(userId to session) }.ifEmpty { null } }
    }

    /** The playlist changed: everyone following it, on any replica, gets the new state shortly. */
    suspend fun changed(playlistId: UUID) {
        val relay = publish ?: return schedule(playlistId)
        try {
            relay(playlistId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn("Could not relay a playlist change: {}", e.message)
            schedule(playlistId)
        }
    }

    /** Pushes [playlistId] to the sockets held by this process. */
    fun schedule(playlistId: UUID) {
        if (!scheduled.add(playlistId)) return
        scope.launch {
            delay(COALESCE_MS)
            scheduled.remove(playlistId)
            push(playlistId)
        }
    }

    private suspend fun push(playlistId: UUID) {
        val targets = sessions[playlistId]?.toList().orEmpty()
        if (targets.isEmpty()) return
        val states = try {
            render(playlistId, targets.map { it.first }.toSet())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn("Could not render playlist {} for its followers: {}", playlistId, e.message)
            return
        }
        targets.forEach { (userId, session) ->
            val state = states[userId]
            val event = if (state != null) PlaylistLiveEvent.State(state) else PlaylistLiveEvent.Gone
            send(session, MusicRoomJson.encodeToString(PlaylistLiveEvent.serializer(), event))
            if (state == null) {
                leave(playlistId, userId, session)
                runCatching { session.close(CloseReason(CloseReason.Codes.NORMAL, "gone")) }
            }
        }
    }

    private suspend fun send(session: WebSocketSession, text: String) {
        try {
            session.send(Frame.Text(text))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The socket is closing: its own loop removes it.
            log.debug("Could not push a playlist update: {}", e.message)
        }
    }

    private companion object {
        const val COALESCE_MS = 120L
    }
}
