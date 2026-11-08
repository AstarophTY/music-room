package com.musicroom.app.player

import com.musicroom.app.music.AppleMusicPlayback
import com.musicroom.shared.dto.TrackResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Owns the asynchronous lifecycle of one device-local Apple Music playback. */
class AppleMusicPlaybackSession(
    private val playback: AppleMusicPlayback,
    private val scope: CoroutineScope,
    private val onEnded: () -> Unit,
    private val onError: (Exception) -> Unit,
) : DevicePlaybackSession {
    val available get() = playback.available
    val connected get() = playback.connected
    override val canResume get() = ready
    private var ready = false
        private set
    private var generation = 0L
    private var job: Job? = null
    private var active = false

    fun start(track: TrackResponse) {
        stop()
        active = true
        val current = generation
        job = scope.launch {
            try {
                playback.play(track.uri, onEnded = { scope.launch { if (isCurrent(current)) onEnded() } }, onError = { failure ->
                    scope.launch { if (isCurrent(current)) fail(failure) }
                })
                if (isCurrent(current)) ready = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isCurrent(current)) fail(e)
            }
        }
    }

    override fun pause() {
        if (ready) playback.pause() else cancelLoad()
    }

    override fun resume() = playback.resume()
    override fun seek(positionMs: Long) = playback.seek(positionMs)
    override fun positionMs(): Long = playback.positionMs()

    override fun pauseAtEnd() {
        playback.pause()
        playback.seek(0)
        ready = false
    }

    override fun stop() {
        val wasActive = active
        cancel()
        active = false
        if (wasActive) playback.stop()
    }

    private fun cancelLoad() {
        cancel()
        playback.stop()
    }

    private fun cancel() {
        generation++
        job?.cancel()
        job = null
        ready = false
    }

    private fun isCurrent(value: Long) = active && generation == value
    private fun fail(error: Exception) {
        ready = false
        onError(error)
    }
}
