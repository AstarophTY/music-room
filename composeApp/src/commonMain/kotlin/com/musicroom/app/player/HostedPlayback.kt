package com.musicroom.app.player

import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.TrackSource
import com.musicroom.shared.dto.TrackResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Plays the songs uploaded to Music Room inside the app: downloads the file (the last few stay in memory), then
 * plays it with the platform's [LocalAudioPlayer]. [onEnded] and [onError] run on [scope]; a newer [play] or [stop]
 * cancels what an older one was still loading.
 */
class HostedPlayback(
    private val api: MusicRoomApi,
    private val scope: CoroutineScope,
    private val onEnded: () -> Unit,
    private val onError: (Exception) -> Unit,
) : DevicePlaybackSession {
    override val source = TrackSource.MUSIC_ROOM
    override fun start(track: TrackResponse) = play(track)
    private val audio = LocalAudioPlayer()
    private val cache = LinkedHashMap<String, ByteArray>()
    private var job: Job? = null
    private var generation = 0

    /** Whether the sound should be playing (it may still be loading). */
    private var playing = false

    /** Downloads and plays [track] from [positionMs]. */
    fun play(track: TrackResponse, positionMs: Long = 0) {
        stop()
        val current = ++generation
        playing = true
        job = scope.launch {
            try {
                val id = track.id.removePrefix(TrackResponse.HOSTED_PREFIX)
                val bytes = cache[id] ?: api.hostedTrackAudio(id).also { remember(id, it) }
                if (current != generation) return@launch
                audio.load(bytes) { scope.launch { if (current == generation) onEnded() } }
                if (current != generation) return@launch
                if (positionMs > 0) audio.seek(positionMs)
                if (playing) audio.play()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (current == generation) onError(e)
            }
        }
    }

    override fun pause() {
        playing = false
        audio.pause()
    }

    override fun resume() {
        playing = true
        audio.play()
    }

    override fun seek(positionMs: Long) = audio.seek(positionMs)

    override fun positionMs(): Long = audio.positionMs()

    override fun stop() {
        generation++
        playing = false
        job?.cancel()
        job = null
        audio.release()
    }

    private fun remember(id: String, bytes: ByteArray) {
        cache[id] = bytes
        while (cache.size > CACHED_SONGS) cache.remove(cache.keys.first())
    }

    private companion object {
        const val CACHED_SONGS = 3
    }
}
