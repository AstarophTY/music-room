package com.musicroom.app.events

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.event_location_failed
import com.musicroom.app.generated.resources.event_track_unavailable
import com.musicroom.app.music.MusicPlaybackAvailability
import com.musicroom.app.player.EventRemote
import com.musicroom.app.player.PlayerController
import com.musicroom.shared.ApiException
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.EventDetailResponse
import com.musicroom.shared.dto.EventLiveEvent
import com.musicroom.shared.dto.EventResponse
import com.musicroom.shared.dto.EventStatus
import com.musicroom.shared.dto.EventTrackResponse
import com.musicroom.shared.dto.EventVoteLicense
import com.musicroom.shared.dto.GeoPoint
import com.musicroom.shared.dto.TrackResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * The one event the user listens to, if any. While an event is focused, the user's Spotify follows its music (the
 * same song, at the same moment, paused with it), and songs swiped to the right are proposed to it instead of
 * going to the queue. Focusing stops the user's own music, and the player's buttons drive the event; playing a song
 * of their own makes the user leave it ([PlayerController.lockedByEvent]). With no event focused, the player works
 * on its own as usual.
 * Focusing needs a linked Spotify Premium account ([canFocus]), since it plays on the user's Spotify.
 */
@Stable
class EventFocus(private val api: MusicRoomApi, private val player: PlayerController, private val scope: CoroutineScope) : EventRemote {
    /** The focused event, kept up to date while [run] follows it. */
    var event by mutableStateOf<EventResponse?>(null)
        private set

    /** Live state of the focused event (its queue, the next song...), shown as the player's queue. */
    var detail by mutableStateOf<EventDetailResponse?>(null)
        private set

    /** Confirmation (or problem) of the last song proposed by a swipe, cleared after a few seconds. */
    var notice by mutableStateOf<ProposalNotice?>(null)
        private set

    /** The user linked a Spotify Premium account; set by the screen. Without it, the focus is dropped. */
    var canFocus by mutableStateOf(false)
        private set

    /** Reads the user's position; null on platforms without one. Set by the screen. */
    var locate: (suspend () -> GeoPoint?)? = null

    /** What was last played on the user's Spotify for the event: the song and whether it was playing. */
    private var synced: Pair<String?, Boolean>? = null
    private var listenJob: Job? = null
    private var providers: MusicPlaybackAvailability? = null
    private var nextNoticeId = 0L
    private var noticeJob: Job? = null

    init {
        // While focused, songs added to the queue go to the event.
        player.proposeToEvent = ::propose
        player.leaveEvent = ::unfocus
    }

    /** An owner command (start, pause, skip) is running: the buttons wait for its answer. */
    private var commandRunning by mutableStateOf(false)

    // The player's buttons drive the focused event (see [EventRemote]): its owner starts, pauses and skips the music.
    override val canControl: Boolean get() = event?.isOwner == true && !commandRunning

    override val playing: Boolean get() = event?.status == EventStatus.PLAYING

    override val canSkip: Boolean get() = event?.status?.let { it != EventStatus.WAITING } == true

    override fun togglePlay() {
        val target = event ?: return
        command(target) { if (target.status == EventStatus.PLAYING) api.pauseEvent(target.id) else api.playEvent(target.id) }
    }

    override fun skip() {
        val target = event ?: return
        command(target) { api.skipEvent(target.id) }
    }

    /** Sends an owner command; its answer is the event's new state, and a problem shows in the player. */
    private fun command(target: EventResponse, action: suspend () -> EventDetailResponse) {
        if (commandRunning) return
        commandRunning = true
        scope.launch {
            try {
                val answer = action()
                if (event?.id == target.id) {
                    event = answer.event
                    detail = answer
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                player.reportPlaybackError(UiText.of(e))
            }
            commandRunning = false
        }
    }

    /** Whether a connected provider can play event tracks; false drops the current focus. */
    fun updateProviders(availability: MusicPlaybackAvailability) {
        providers = availability
        canFocus = availability.canListen
        if (!canFocus) unfocus()
    }

    fun focus(newEvent: EventResponse) {
        if (!canFocus || event?.id == newEvent.id) return
        synced = null
        detail = null
        event = newEvent
        player.lockForEvent(true)
        player.eventRemote = this
        // The user's own music stops; the owner's Spotify may already play the event, which goes on.
        val ownerStreaming = newEvent.isOwner && newEvent.status == EventStatus.PLAYING
        if (player.isPlaying && !ownerStreaming) player.forEvent { player.togglePlay() }
    }

    fun unfocus() {
        listenJob?.cancel()
        event = null
        detail = null
        synced = null
        commandRunning = false
        player.eventRemote = null
        player.lockForEvent(false)
    }

    /** Follows the focused event until cancelled, switching when the focus changes. */
    suspend fun run() {
        snapshotFlow { event?.id }.collectLatest { id -> if (id != null) follow(id) }
    }

    /** Proposes [track] to the focused event in the background; [notice] says how it went. */
    fun propose(track: TrackResponse) = propose(listOf(track))

    /** Proposes [tracks] to the focused event in the background, in order; [notice] says how it went. */
    fun propose(tracks: List<TrackResponse>) {
        val target = event ?: return
        if (tracks.isEmpty()) return
        scope.launch {
            val problem = try {
                val location = location(target)
                tracks.forEach { track -> detail = api.proposeEventTrack(target.id, track, location) }
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: LocationUnavailable) {
                UiText.of(Res.string.event_location_failed)
            } catch (e: Exception) {
                UiText.of(e)
            }
            showNotice(ProposalNotice(nextNoticeId++, tracks.first(), tracks.size, target.name, problem))
        }
    }

    /** Votes for the song [entryId] of the focused event's queue, or takes the vote back. */
    fun toggleVote(entryId: String) {
        val target = event ?: return
        val current = detail ?: return
        val entry = current.queue.firstOrNull { it.id == entryId } ?: return
        val voting = !entry.voted
        // Shown right away, most voted first (the oldest first among equals, as the server keeps them).
        val order = current.queue.map { it.id }
        val updated = current.queue.map {
            if (it.id == entryId) it.copy(voted = voting, votes = it.votes + if (voting) 1 else -1) else it
        }
        detail = current.copy(queue = updated.sortedWith(compareByDescending<EventTrackResponse> { it.votes }.thenBy { order.indexOf(it.id) }))
        scope.launch {
            val problem = try {
                detail = if (voting) {
                    api.voteEventTrack(target.id, entryId, location(target))
                } else {
                    api.unvoteEventTrack(target.id, entryId)
                }
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: LocationUnavailable) {
                UiText.of(Res.string.event_location_failed)
            } catch (e: Exception) {
                UiText.of(e)
            }
            if (problem != null) {
                if (event?.id == target.id) detail = current
                showNotice(ProposalNotice(nextNoticeId++, entry.track, 1, target.name, problem))
            }
        }
    }

    /** Live state of the event [id], reconnecting after a network problem, until the focus moves. */
    private suspend fun follow(id: String) {
        var pause = 1_000L
        while (true) {
            try {
                api.eventLive(id) { live ->
                    pause = 1_000L
                    when (live) {
                        is EventLiveEvent.State -> {
                            event = live.detail.event
                            detail = live.detail
                            listenAlong(live.detail)
                        }
                        EventLiveEvent.Deleted -> unfocus()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (e.status == 404) {
                    unfocus()
                    return
                }
            } catch (e: Exception) {
                // Network problem: try again below.
            }
            delay(pause)
            pause = (pause * 2).coerceAtMost(30_000)
        }
    }

    /**
     * Plays on the user's Spotify what the event plays, when the song changes or the owner pauses or resumes it.
     * The owner's own Spotify already plays it.
     */
    private fun listenAlong(detail: EventDetailResponse) {
        val track = detail.nowPlaying?.track
        if (detail.event.isOwner && track != null && providers?.hostControlsPlayback(track) == true) {
            listenJob?.cancel()
            synced = track.id to (detail.event.status == EventStatus.PLAYING)
            player.stopEventLocalPlayback()
            return
        }
        val playing = track != null && detail.event.status == EventStatus.PLAYING
        val target = track?.id to playing
        if (target == synced) return
        val previous = synced
        synced = target
        listenJob?.cancel()
        when {
            playing && track != null -> {
                listenJob = scope.launch {
                    if (player.isPlaying && player.track?.id != track.id) {
                        player.forEvent { player.togglePlay() }
                    }
                    val playable = try {
                        resolveTrack(track)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                    if (playable == null) {
                        player.reportPlaybackError(UiText.of(Res.string.event_track_unavailable))
                        return@launch
                    }
                    if (event?.id != detail.event.id || synced != target) return@launch
                    player.forEvent { player.play(listOf(playable), 0) }
                    delay(SEEK_DELAY_MS)
                    if (event?.id != detail.event.id || synced != target) return@launch
                    player.forEvent { player.seek(detail.progressMs + (Clock.System.now().toEpochMilliseconds() - detail.sentAt)) }
                }
            }
            // Paused or stopped by the owner: pause too.
            previous?.second == true && player.isPlaying -> player.forEvent { player.togglePlay() }
        }
    }

    private suspend fun resolveTrack(track: TrackResponse): TrackResponse? =
        providers?.resolve(track)

    /** The position to send for [target], when its votes need one; throws [LocationUnavailable] when unknown. */
    private suspend fun location(target: EventResponse): GeoPoint? =
        if (target.voteLicense == EventVoteLicense.LOCATION && !target.isOwner) locateOrNull() ?: throw LocationUnavailable() else null

    private suspend fun locateOrNull(): GeoPoint? = try {
        locate?.invoke()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private fun showNotice(newNotice: ProposalNotice) {
        noticeJob?.cancel()
        notice = newNotice
        noticeJob = scope.launch {
            delay(NOTICE_MS)
            notice = null
        }
    }

    /** The position needed to propose to the event could not be read. */
    private class LocationUnavailable : Exception()

    /** [count] songs (the first: [track]) proposed to the event [eventName]; [problem] when the server refused it. */
    data class ProposalNotice(val id: Long, val track: TrackResponse, val count: Int, val eventName: String, val problem: UiText?)

    companion object {
        /** The focus of the screen, for the song rows that propose to it. */
        val Local: ProvidableCompositionLocal<EventFocus?> = staticCompositionLocalOf { null }

        private const val SEEK_DELAY_MS = 1_500L
        private const val NOTICE_MS = 3_500L
    }
}
