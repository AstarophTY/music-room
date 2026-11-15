package com.musicroom.app.player

import com.musicroom.shared.dto.PlaybackStateResponse
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull

/** Polls remote playback and suppresses stale responses while commands are taking effect. */
class RemotePlaybackSynchronizer {
    private val refresh = Channel<Unit>(Channel.CONFLATED)
    private var pendingTrackId: String? = null
    private var pendingUntil: TimeSource.Monotonic.ValueTimeMark? = null
    private var pendingDevice: String? = null
    private var pendingPlaying = false

    val waiting: Boolean get() = pendingUntil != null

    fun waitFor(trackId: String?, device: String? = null, playing: Boolean = false) {
        pendingTrackId = trackId
        pendingDevice = device
        pendingPlaying = playing
        pendingUntil = TimeSource.Monotonic.markNow() + 8.seconds
    }

    fun stopWaiting() {
        pendingTrackId = null
        pendingDevice = null
        pendingPlaying = false
        pendingUntil = null
    }

    fun isStillStarting(fetched: PlaybackStateResponse): Boolean {
        val until = pendingUntil ?: return false
        val expected = pendingTrackId
        val started = fetched.track != null && (expected == null || fetched.track?.id == expected) &&
            (pendingDevice == null || fetched.deviceName == pendingDevice) && (!pendingPlaying || fetched.isPlaying)
        if (started || until.hasPassedNow()) {
            stopWaiting()
            return false
        }
        return true
    }

    fun requestRefresh() { refresh.trySend(Unit) }

    suspend fun run(fetch: suspend () -> PlaybackStateResponse, onState: (PlaybackStateResponse) -> Unit, interval: () -> Duration) {
        while (true) {
            try {
                onState(fetch())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Keep the last known state when a poll fails.
            }
            withTimeoutOrNull(interval()) { refresh.receive() }
        }
    }
}
