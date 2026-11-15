package com.musicroom.server.modules.events

import com.musicroom.server.common.exceptions.HttpException
import com.musicroom.server.modules.events.entities.Event
import com.musicroom.server.modules.events.entities.EventTrack
import com.musicroom.server.modules.events.entities.EventTracksTable
import com.musicroom.shared.dto.EventStatus
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory

/**
 * Plays an event's music on its owner's Spotify: only the owner streams, the other people only propose and vote.
 * When the owner has no linked Spotify Premium account, the event still runs on a clock instead ([streams]): each
 * song "plays" for its length, then the most voted one follows, and the people listening to the event hear it on
 * their own Spotify.
 *
 * Every few seconds ([run]) it looks at what the owner's Spotify plays. Shortly before the current song ends, the
 * most voted queued song is handed to Spotify's queue (it becomes "next" and stops moving); when Spotify starts it,
 * it becomes "playing" and the previous one "played". When nothing of the event plays (just started, the queue was
 * empty when the song ended...), the most voted song starts right away. One lock per event keeps the owner's
 * commands and the checks from overlapping.
 */
class EventPlayer(
    private val events: EventsRepository,
    private val tracks: EventTracksRepository,
    private val providers: EventPlaybackProviders,
    private val onChange: (UUID) -> Unit,
) {
    private val log = LoggerFactory.getLogger(EventPlayer::class.java)
    private val locks = ConcurrentHashMap<UUID, Mutex>()

    /** Why the music cannot play right now (an ApiErrors code), by event: shown to the owner. */
    private val issues = ConcurrentHashMap<UUID, String>()

    /** Where the song was at the last check, and when (epoch ms), by event: the page shows it without asking Spotify. */
    private val positions = ConcurrentHashMap<UUID, Pair<Long, Long>>()

    /** When the player last started a song, by event: Spotify takes a few seconds to report it. */
    private val startedAt = ConcurrentHashMap<UUID, TimeSource.Monotonic.ValueTimeMark>()

    fun issue(eventId: UUID): String? = issues[eventId]

    /** Checks the playing events until [scope] ends (the server stops). */
    fun run(scope: CoroutineScope): Job = scope.launch {
        while (isActive) {
            delay(TICK_MS)
            val ids = try {
                events.playing()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.warn("Could not list the playing events: {}", e.message)
                continue
            }
            ids.forEach { id ->
                try {
                    tick(id)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.warn("Event {} playback check failed: {}", id, e.message)
                }
            }
        }
    }

    /** One check of [eventId] (see the class). Spotify problems are kept in [issue], not thrown. */
    suspend fun tick(eventId: UUID) = lock(eventId).withLock {
        val event = events.find(eventId) ?: return@withLock
        if (event.status != EventStatus.PLAYING) return@withLock
        try {
            check(event)
            clearIssue(event.id)
        } catch (e: HttpException) {
            setIssue(event.id, e.error)
        }
    }

    /** Starts the music, or resumes it after a pause. Throws when Spotify cannot play (shown to the owner). */
    suspend fun start(event: Event) = lock(event.id).withLock {
        val current = tracks.withStatus(event.id, EventTracksTable.PLAYING)
        when {
            event.status == EventStatus.PAUSED && current != null -> {
                remote(event, current)?.play(event.ownerId)
                // The clock goes on from where it was paused.
                positions[event.id] = progressMs(event) to System.currentTimeMillis()
            }
            current != null -> playTrack(event, current)
            else -> playTop(event, current = null, next = tracks.withStatus(event.id, EventTracksTable.NEXT))
        }
        events.setStatus(event.id, EventStatus.PLAYING)
        clearIssue(event.id)
        onChange(event.id)
    }

    suspend fun pause(event: Event) = lock(event.id).withLock {
        val current = tracks.withStatus(event.id, EventTracksTable.PLAYING)
        remote(event, current)?.pause(event.ownerId)
        positions[event.id] = progressMs(event) to System.currentTimeMillis()
        events.setStatus(event.id, EventStatus.PAUSED)
        onChange(event.id)
    }

    /** Plays the most voted song now; the current one counts as played. */
    suspend fun skip(event: Event) = lock(event.id).withLock {
        playTop(event, tracks.withStatus(event.id, EventTracksTable.PLAYING), tracks.withStatus(event.id, EventTracksTable.NEXT))
        if (event.status != EventStatus.PLAYING) events.setStatus(event.id, EventStatus.PLAYING)
        clearIssue(event.id)
        onChange(event.id)
    }

    /** Stops the event's music: the current song counts as played, the next one goes back to the vote. */
    suspend fun stop(event: Event) = lock(event.id).withLock {
        try {
            val current = tracks.withStatus(event.id, EventTracksTable.PLAYING)
            remote(event, current)?.pause(event.ownerId)
        } catch (e: HttpException) {
            // Nothing to pause on Spotify: stopping the event is enough.
        }
        tracks.withStatus(event.id, EventTracksTable.PLAYING)?.let { tracks.setStatus(it.id, EventTracksTable.PLAYED) }
        tracks.withStatus(event.id, EventTracksTable.NEXT)?.let { tracks.setStatus(it.id, EventTracksTable.QUEUED) }
        events.setStatus(event.id, EventStatus.WAITING)
        clearIssue(event.id)
        onChange(event.id)
    }

    /** Where the song playing is, from the last check (0 when unknown). It moves on only while the music plays. */
    fun progressMs(event: Event): Long {
        if (event.status == EventStatus.WAITING) return 0
        val (progress, at) = positions[event.id] ?: return 0
        return if (event.status == EventStatus.PLAYING) progress + (System.currentTimeMillis() - at) else progress
    }

    fun forget(eventId: UUID) {
        locks.remove(eventId)
        issues.remove(eventId)
        startedAt.remove(eventId)
        positions.remove(eventId)
    }

    private suspend fun check(event: Event) {
        val currentTrack = tracks.withStatus(event.id, EventTracksTable.PLAYING)
        val remote = remote(event, currentTrack) ?: return checkClock(event)
        val state = remote.state(event.ownerId)
        positions[event.id] = state.progressMs to System.currentTimeMillis()
        val current = tracks.withStatus(event.id, EventTracksTable.PLAYING)
        val next = tracks.withStatus(event.id, EventTracksTable.NEXT)
        val playingId = state.track?.id
        when {
            // Spotify moved on to the song handed to it.
            next != null && playingId == next.track.id -> {
                current?.let { tracks.setStatus(it.id, EventTracksTable.PLAYED) }
                tracks.setStatus(next.id, EventTracksTable.PLAYING)
                onChange(event.id)
            }
            current != null && playingId == current.track.id -> {
                val remaining = current.track.durationMs - state.progressMs
                val ended = !state.isPlaying && (state.progressMs == 0L || remaining <= END_MARGIN_MS)
                when {
                    // Finished with nothing handed to Spotify (the queue was empty then): play the best one now.
                    ended -> playTop(event, current, next)
                    next == null && state.isPlaying && remaining <= QUEUE_BEFORE_END_MS -> queueTop(event)
                }
            }
            // Spotify still reports what played before the song just started.
            startedRecently(event.id) -> Unit
            // Nothing of the event plays: start the best song (if any).
            else -> if (current != null || next != null || tracks.top(event.id) != null) playTop(event, current, next)
        }
    }

    /** Without the owner's Spotify: the song ends when its length has passed, and the best one follows. */
    private suspend fun checkClock(event: Event) {
        val current = tracks.withStatus(event.id, EventTracksTable.PLAYING)
        val next = tracks.withStatus(event.id, EventTracksTable.NEXT)
        when {
            current == null -> if (next != null || tracks.top(event.id) != null) playTop(event, null, next)
            progressMs(event) >= current.track.durationMs -> playTop(event, current, next)
        }
    }

    private suspend fun remote(event: Event, track: EventTrack?): EventPlaybackProviders.Provider? =
        providers.forTrack(track?.track)?.takeIf { it.available(event.ownerId) }

    /** Hands the most voted song to the queue of the provider playing now: it plays after the current one. */
    private suspend fun queueTop(event: Event) {
        val top = tracks.top(event.id) ?: return
        val current = tracks.withStatus(event.id, EventTracksTable.PLAYING)
        val provider = remote(event, current) ?: return
        // A different provider cannot be prequeued remotely; playTop handles it when the current track ends.
        if (providers.forTrack(top.track) !== provider) return
        tracks.setStatus(top.id, EventTracksTable.NEXT)
        try {
            provider.enqueue(event.ownerId, top.track)
        } catch (e: HttpException) {
            tracks.setStatus(top.id, EventTracksTable.QUEUED)
            throw e
        }
        onChange(event.id)
    }

    /** Plays the song already chosen ([next]) or the most voted one now; [current] counts as played. */
    private suspend fun playTop(event: Event, current: EventTrack?, next: EventTrack?) {
        val candidate = next ?: tracks.top(event.id)
        if (candidate != null) playTrack(event, candidate)
        current?.takeIf { it.id != candidate?.id }?.let { tracks.setStatus(it.id, EventTracksTable.PLAYED) }
        onChange(event.id)
    }

    private suspend fun playTrack(event: Event, track: EventTrack) {
        val provider = providers.forTrack(track.track)
        if (provider != null) {
            if (provider.available(event.ownerId)) provider.play(event.ownerId, track.track)
        } else {
            val current = tracks.withStatus(event.id, EventTracksTable.PLAYING)
            remote(event, current)?.pause(event.ownerId)
        }
        tracks.setStatus(track.id, EventTracksTable.PLAYING)
        startedAt[event.id] = TimeSource.Monotonic.markNow()
        positions[event.id] = 0L to System.currentTimeMillis()
    }

    private fun startedRecently(eventId: UUID) =
        startedAt[eventId]?.elapsedNow()?.inWholeMilliseconds?.let { it < START_GRACE_MS } == true

    private fun setIssue(eventId: UUID, error: String) {
        if (issues.put(eventId, error) != error) onChange(eventId)
    }

    private fun clearIssue(eventId: UUID) {
        if (issues.remove(eventId) != null) onChange(eventId)
    }

    private fun lock(eventId: UUID) = locks.computeIfAbsent(eventId) { Mutex() }

    private companion object {
        const val TICK_MS = 4_000L

        /** The next song is handed to Spotify this long before the end (checks are [TICK_MS] apart). */
        const val QUEUE_BEFORE_END_MS = 15_000L

        /** A paused song this close to its end has ended. */
        const val END_MARGIN_MS = 2_000L

        const val START_GRACE_MS = 10_000L

    }
}
