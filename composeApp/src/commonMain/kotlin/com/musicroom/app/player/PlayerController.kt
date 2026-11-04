package com.musicroom.app.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.musicroom.app.common.UiText
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.ApiException
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.PlaybackStateResponse
import com.musicroom.shared.dto.PlaylistSummaryResponse
import com.musicroom.shared.dto.TrackResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Remote for Spotify playback on the user's devices (Spotify Connect): the music plays in the Spotify app
 * (phone, computer, speaker) and this controls it. Commands update the state right away, then [sync]
 * confirms it with Spotify.
 */
@Stable
class PlayerController(private val api: MusicRoomApi, private val scope: CoroutineScope) {
    var state by mutableStateOf<PlaybackStateResponse?>(null)
        private set

    var error by mutableStateOf<UiText?>(null)
        private set

    /** The last error can be fixed by opening Spotify on a device. */
    var needsDevice by mutableStateOf(false)
        private set

    private var syncedAt by mutableStateOf(TimeSource.Monotonic.markNow())
    private val refreshNow = Channel<Unit>(Channel.CONFLATED)

    val track: TrackResponse? get() = state?.track
    val isPlaying: Boolean get() = state?.isPlaying == true

    /** Position in the track, moving forward between two syncs while it plays. */
    fun positionMs(): Long {
        val current = state ?: return 0
        val elapsed = if (current.isPlaying) syncedAt.elapsedNow().inWholeMilliseconds else 0
        val position = current.progressMs + elapsed
        return current.track?.durationMs?.let { position.coerceAtMost(it) } ?: position
    }

    /** Plays [tracks] from [index]; the rest of the list follows as the queue. */
    fun play(tracks: List<TrackResponse>, index: Int) {
        val track = tracks.getOrNull(index) ?: return
        update(PlaybackStateResponse(active = true, isPlaying = true, track = track, deviceName = state?.deviceName))
        command { api.play(tracks.map { it.uri }, index) }
    }

    /** Plays [playlist] as a whole from [index]; [tracks] (when Spotify listed them) shows the track right away. */
    fun playPlaylist(playlist: PlaylistSummaryResponse, tracks: List<TrackResponse>, index: Int) {
        val uri = playlist.uri ?: return play(tracks, index)
        tracks.getOrNull(index)?.let {
            update(PlaybackStateResponse(active = true, isPlaying = true, track = it, deviceName = state?.deviceName))
        }
        command { api.play(index = index, contextUri = uri) }
    }

    fun togglePlay() {
        val current = state ?: return
        update(current.copy(isPlaying = !current.isPlaying, progressMs = positionMs()))
        command { if (current.isPlaying) api.pause() else api.play() }
    }

    fun next() = command { api.skipNext() }

    fun previous() = command { api.skipPrevious() }

    fun seek(positionMs: Long) {
        state?.let { update(it.copy(progressMs = positionMs)) }
        command { api.seek(positionMs) }
    }

    fun dismissError() {
        error = null
        needsDevice = false
    }

    /** Keeps the state in sync with Spotify until cancelled: often while playing, rarely otherwise. */
    suspend fun sync() {
        while (true) {
            try {
                update(api.playbackState())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Keep the last known state; a failed poll is not worth an error message.
            }
            withTimeoutOrNull(if (isPlaying) 5.seconds else 15.seconds) { refreshNow.receive() }
        }
    }

    private fun update(newState: PlaybackStateResponse) {
        state = newState
        syncedAt = TimeSource.Monotonic.markNow()
    }

    private fun command(action: suspend () -> Unit) {
        scope.launch {
            try {
                action()
                dismissError()
                // Spotify takes a moment to apply a command before its state shows it.
                delay(700.milliseconds)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = UiText.of(e)
                needsDevice = (e as? ApiException)?.error == ApiErrors.SPOTIFY_NO_DEVICE
            }
            refreshNow.trySend(Unit)
        }
    }

    companion object {
        /** Opens the Spotify app on phones; on computers the web player, which also counts as a device. */
        const val SPOTIFY_WEB_URL = "https://open.spotify.com"
    }
}
