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
import com.musicroom.app.player.PlayerController
import com.musicroom.shared.ApiException
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.EventDetailResponse
import com.musicroom.shared.dto.EventLiveEvent
import com.musicroom.shared.dto.EventResponse
import com.musicroom.shared.dto.EventStatus
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
 * going to the queue. Focusing stops the user's own music, and the player refuses their own commands until they
 * stop listening ([PlayerController.lockedByEvent]). With no event focused, the player works on its own as usual.
 * Focusing needs a linked Spotify Premium account ([canFocus]), since it plays on the user's Spotify.
 */
@Stable
class EventFocus(private val api: MusicRoomApi, private val player: PlayerController, private val scope: CoroutineScope) {
    /** The focused event, kept up to date while [run] follows it. */
    var event by mutableStateOf<EventResponse?>(null)
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
    private var nextNoticeId = 0L
    private var noticeJob: Job? = null

    /** Whether the user can focus an event (Spotify Premium linked); false drops the current focus. */
    fun updateCanFocus(premium: Boolean) {
        canFocus = premium
        if (!premium) unfocus()
    }

    fun focus(newEvent: EventResponse) {
        if (!canFocus || event?.id == newEvent.id) return
        synced = null
        event = newEvent
        player.lockForEvent(true)
        // The user's own music stops; the owner's Spotify may already play the event, which goes on.
        val ownerStreaming = newEvent.isOwner && newEvent.status == EventStatus.PLAYING
        if (player.isPlaying && !ownerStreaming) player.forEvent { player.togglePlay() }
    }

    fun unfocus() {
        event = null
        synced = null
        player.lockForEvent(false)
    }

    /** Follows the focused event until cancelled, switching when the focus changes. */
    suspend fun run() {
        snapshotFlow { event?.id }.collectLatest { id -> if (id != null) follow(id) }
    }

    /** Proposes [track] to the focused event in the background; [notice] says how it went. */
    fun propose(track: TrackResponse) {
        val target = event ?: return
        scope.launch {
            val problem = try {
                val location = if (target.voteLicense == EventVoteLicense.LOCATION && !target.isOwner) {
                    locateOrNull() ?: throw LocationUnavailable()
                } else {
                    null
                }
                api.proposeEventTrack(target.id, track.id, location)
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: LocationUnavailable) {
                UiText.of(Res.string.event_location_failed)
            } catch (e: Exception) {
                UiText.of(e)
            }
            showNotice(ProposalNotice(nextNoticeId++, track, target.name, problem))
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
        if (detail.event.isOwner || !player.spotifyConnected) return
        val track = detail.nowPlaying?.track
        val playing = track != null && detail.event.status == EventStatus.PLAYING
        val target = track?.id to playing
        if (target == synced) return
        val previous = synced
        synced = target
        when {
            playing && track != null -> {
                player.forEvent { player.play(listOf(track), 0) }
                scope.launch {
                    // Spotify needs a moment to start the song before it can jump to the event's moment.
                    delay(SEEK_DELAY_MS)
                    if (event?.id != detail.event.id) return@launch
                    player.forEvent { player.seek(detail.progressMs + (Clock.System.now().toEpochMilliseconds() - detail.sentAt)) }
                }
            }
            // Paused or stopped by the owner: pause too.
            previous?.second == true && player.isPlaying -> player.forEvent { player.togglePlay() }
        }
    }

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

    /** [track] proposed by a swipe to the event [eventName]; [problem] when the server refused it. */
    data class ProposalNotice(val id: Long, val track: TrackResponse, val eventName: String, val problem: UiText?)

    companion object {
        /** The focus of the screen, for the song rows that propose to it. */
        val Local: ProvidableCompositionLocal<EventFocus?> = staticCompositionLocalOf { null }

        private const val SEEK_DELAY_MS = 1_500L
        private const val NOTICE_MS = 3_500L
    }
}
