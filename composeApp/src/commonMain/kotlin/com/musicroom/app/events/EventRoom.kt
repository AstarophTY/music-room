package com.musicroom.app.events

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.event_location_failed
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.ApiException
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.EventDetailResponse
import com.musicroom.shared.dto.EventLiveEvent
import com.musicroom.shared.dto.EventTrackResponse
import com.musicroom.shared.dto.EventVoteLicense
import com.musicroom.shared.dto.GeoPoint
import com.musicroom.shared.dto.TrackResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * One event as its page shows it: the state from the server (kept live by [run]) and the user's actions. A vote
 * shows at once and is corrected by the server's answer. With the location license, the user's position is read
 * ([locate]) and sent with each proposal and vote.
 */
class EventRoom(private val api: MusicRoomApi, val eventId: String, private val scope: CoroutineScope) {
    var detail by mutableStateOf<EventDetailResponse?>(null)
        private set

    var error by mutableStateOf<UiText?>(null)

    /** The owner deleted the event while it was open. */
    var deleted by mutableStateOf(false)
        private set

    /** An owner command is running (play, pause...). */
    var busy by mutableStateOf(false)
        private set

    /** Reads the user's position; null on platforms without one. Set by the page. */
    var locate: (suspend () -> GeoPoint?)? = null

    /** The owner tried to play without a linked Spotify account. Set by the page. */
    var onSpotifyNeeded: () -> Unit = {}

    /** Follows the event until cancelled (the page closes), reconnecting after a network problem. */
    suspend fun run() {
        var pause = 1_000L
        while (!deleted) {
            try {
                api.eventLive(eventId) { event ->
                    pause = 1_000L
                    when (event) {
                        is EventLiveEvent.State -> {
                            detail = event.detail
                            error = null
                        }
                        EventLiveEvent.Deleted -> deleted = true
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (e.status == 404) {
                    deleted = true
                    return
                }
                if (detail == null) error = UiText.of(e)
            } catch (e: Exception) {
                if (detail == null) error = UiText.of(e)
            }
            if (deleted) return
            delay(pause)
            pause = (pause * 2).coerceAtMost(30_000)
        }
    }

    /** Votes for the song [trackId], or takes the vote back. */
    fun toggleVote(trackId: String) {
        val current = detail ?: return
        val track = current.queue.firstOrNull { it.id == trackId } ?: return
        val voting = !track.voted
        // Shown right away, most voted first (the oldest first among equals, as the server keeps them).
        val updated = current.queue.map {
            if (it.id == trackId) it.copy(voted = voting, votes = it.votes + if (voting) 1 else -1) else it
        }
        val order = current.queue.map { it.id }
        detail = current.copy(queue = updated.sortedWith(compareByDescending<EventTrackResponse> { it.votes }.thenBy { order.indexOf(it.id) }))
        send(revertTo = current) {
            if (voting) {
                val (ok, location) = position()
                if (!ok) return@send null
                api.voteEventTrack(eventId, trackId, location)
            } else {
                api.unvoteEventTrack(eventId, trackId)
            }
        }
    }

    /** Proposes [track] (a vote for it when it already waits). Throws so the picker shows the problem. */
    suspend fun propose(track: TrackResponse) {
        val (ok, location) = position()
        if (!ok) throw ApiException(400, ApiErrors.EVENT_LOCATION_REQUIRED, null)
        detail = api.proposeEventTrack(eventId, track.id, location)
    }

    fun remove(trackId: String) = send { api.removeEventTrack(eventId, trackId) }

    fun play() = command { api.playEvent(eventId) }

    fun pause() = command { api.pauseEvent(eventId) }

    fun skip() = command { api.skipEvent(eventId) }

    fun stop() = command { api.stopEvent(eventId) }

    fun invite(userId: String) = send { api.inviteToEvent(eventId, userId) }

    fun uninvite(userId: String) = send {
        api.uninviteFromEvent(eventId, userId)
        api.event(eventId)
    }

    fun replace(newDetail: EventDetailResponse) {
        detail = newDetail
    }

    /**
     * Whether the user can go on, and the position to send: (true, null) when the event does not need one (or the
     * user owns it), (false, null) with an error shown when it is needed but unknown.
     */
    private suspend fun position(): Pair<Boolean, GeoPoint?> {
        val event = detail?.event ?: return true to null
        if (event.voteLicense != EventVoteLicense.LOCATION || event.isOwner) return true to null
        val found = try {
            locate?.invoke()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        if (found == null) error = UiText.of(Res.string.event_location_failed)
        return (found != null) to found
    }

    /** Runs an owner command; a missing Spotify account asks to link it. */
    private fun command(action: suspend () -> EventDetailResponse) {
        busy = true
        scope.launch {
            try {
                detail = action()
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if ((e as? ApiException)?.error == ApiErrors.SPOTIFY_NOT_CONNECTED) onSpotifyNeeded() else error = UiText.of(e)
            }
            busy = false
        }
    }

    /** Sends a change; its answer is the new state. On failure, goes back to [revertTo] and shows the problem. */
    private fun send(revertTo: EventDetailResponse? = null, action: suspend () -> EventDetailResponse?) {
        scope.launch {
            try {
                val answer = action()
                if (answer != null) {
                    detail = answer
                    error = null
                } else if (revertTo != null) {
                    detail = revertTo
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (revertTo != null) detail = revertTo
                error = UiText.of(e)
            }
        }
    }
}
