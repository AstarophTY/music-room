package com.musicroom.app

import com.musicroom.app.music.AppleMusicPlayback
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.MediaPlayer.MPMusicPlaybackState
import platform.MediaPlayer.MPMusicPlayerController
import platform.MediaPlayer.MPMusicPlayerControllerPlaybackStateDidChangeNotification
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Plays Apple Music songs through the Music app of the device.
 *
 * The player needs no token of its own: it plays the catalog with the Apple Music subscription the device is
 * signed in to, which [IosAppleMusicConnection] asked permission for. A song played to its end leaves a queue
 * of one empty, which is what tells the song is over. Songs started while a previous one was still being
 * prepared are dropped ([generation]), so the last requested song is the one heard.
 */
class IosAppleMusicPlayback(private val connection: IosAppleMusicConnection) : AppleMusicPlayback {
    override val available = true
    override val connected get() = connection.connected
    private val player = MPMusicPlayerController.applicationMusicPlayer
    private var generation = 0L
    private var request: Request? = null

    /** Set once the song actually started, so that only a song of ours ending reports an end. */
    private var playing = false
    private var observer: Any? = null

    /** [onError] goes unused: the player reports its failures when preparing a song, which throws instead. */
    override suspend fun play(songId: String, onEnded: () -> Unit, onError: (Exception) -> Unit) {
        val current = ++generation
        if (!connection.connected) error("Connect Apple Music before playback")
        observeEnd()
        request = Request(current, onEnded)
        player.setQueueWithStoreIDs(listOf(songId))
        try {
            prepare()
        } catch (e: Exception) {
            if (current == generation) request = null
            throw e
        }
        // Another song was asked for while this one was being prepared: that one wins.
        if (current != generation) return
        player.play()
        playing = true
    }

    override fun pause() {
        player.pause()
    }

    override fun resume() {
        player.play()
    }

    override fun seek(positionMs: Long) {
        player.currentPlaybackTime = positionMs / 1000.0
    }

    /** The player answers NaN while it holds no song, for instance between two of them. */
    override fun positionMs(): Long = player.currentPlaybackTime.takeIf { !it.isNaN() }?.let { (it * 1000).toLong() } ?: 0

    override fun stop() {
        generation++
        request = null
        playing = false
        player.stop()
    }

    private suspend fun prepare(): Unit = suspendCancellableCoroutine { continuation ->
        player.prepareToPlayWithCompletionHandler { error ->
            if (error == null) {
                continuation.resume(Unit)
            } else {
                continuation.resumeWithException(IllegalStateException("Apple Music playback failed: ${error.localizedDescription}"))
            }
        }
    }

    /** The player only posts its state changes once asked to, and keeps doing so for the whole app. */
    private fun observeEnd() {
        if (observer != null) return
        player.beginGeneratingPlaybackNotifications()
        observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = MPMusicPlayerControllerPlaybackStateDidChangeNotification,
            `object` = player,
            queue = NSOperationQueue.mainQueue,
        ) {
            if (playing && player.playbackState == MPMusicPlaybackState.MPMusicPlaybackStateStopped) {
                playing = false
                current()?.onEnded?.invoke()
            }
        }
    }

    /** The song being played now, or null when the state change belongs to a song already replaced or stopped. */
    private fun current(): Request? = request?.takeIf { it.generation == generation }

    private class Request(val generation: Long, val onEnded: () -> Unit)
}
