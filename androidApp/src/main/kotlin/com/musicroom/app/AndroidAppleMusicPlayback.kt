package com.musicroom.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.apple.android.music.playback.controller.MediaPlayerController
import com.apple.android.music.playback.controller.MediaPlayerControllerFactory
import com.apple.android.music.playback.model.MediaItemType
import com.apple.android.music.playback.model.MediaPlayerException
import com.apple.android.music.playback.model.PlayerQueueItem
import com.apple.android.music.playback.queue.CatalogPlaybackQueueItemProvider
import com.apple.android.music.playback.queue.PlaybackModeContainer
import com.apple.android.music.playback.queue.PlaybackQueueItemsAdded
import com.apple.android.sdk.authentication.TokenProvider
import com.musicroom.app.music.AppleMusicPlayback
import com.musicroom.shared.MusicRoomApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

/**
 * Plays Apple Music songs on the device with the MusicKit SDK.
 *
 * One player is built on the first song and then kept: each song replaces its queue. The developer token the SDK
 * asks for (at any moment, from its own threads) is fetched once and refreshed in the background, so a song paused
 * for longer than the token's hour still resumes. Songs started while a previous one was still loading are dropped
 * ([generation]), so the last requested song is the one heard.
 */
class AndroidAppleMusicPlayback(
    private val context: Context,
    private val connection: AndroidAppleMusicConnection,
    private val api: MusicRoomApi,
) : AppleMusicPlayback {
    override val available = true
    override val connected get() = connection.connected
    private var controller: MediaPlayerController? = null
    private var generation = 0L
    private var request: Request? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var refreshJob: Job? = null
    @Volatile private var developerToken: String = ""
    private var tokenAt: TimeSource.Monotonic.ValueTimeMark? = null

    /** The account behind the player is gone (token revoked): the next song builds a new one. */
    private var authorizationLost = false

    private val tokens = object : TokenProvider {
        // Qualified on purpose: TokenProvider is a Java interface, so a bare `developerToken` reads this
        // object's own getter through its synthetic property and calls itself until the stack is gone.
        override fun getDeveloperToken(): String = this@AndroidAppleMusicPlayback.developerToken

        // Read on each call: reconnecting gives a new token without rebuilding the player.
        override fun getUserToken(): String = connection.musicUserToken().orEmpty()
        override fun getAppleMusicStoreFrontId(): String? = connection.appleStorefront()
    }

    override suspend fun play(songId: String, onEnded: () -> Unit, onError: (Exception) -> Unit) {
        val current = ++generation
        try {
            loadSdk()
        } catch (e: UnsatisfiedLinkError) {
            throw IllegalStateException("Apple Music playback is not supported on this device", e)
        }
        if (connection.musicUserToken().isNullOrBlank()) error("Connect Apple Music before playback")
        refreshDeveloperToken()
        currentCoroutineContext().ensureActive()
        // Another song was asked for while the token was fetched: that one wins.
        if (current != generation) return
        request = Request(current, onEnded, onError)
        val player = player()
        try {
            player.prepare(CatalogPlaybackQueueItemProvider.Builder().items(MediaItemType.SONG, songId).build(), true)
        } catch (e: Exception) {
            if (current == generation) request = null
            throw e
        }
    }

    /** Loads the SDK's native libraries, as its sample app does; playback fails without them. */
    private fun loadSdk() {
        System.setProperty("org.bytedeco.javacpp.maxphysicalbytes", "0")
        System.setProperty("org.bytedeco.javacpp.maxbytes", "0")
        System.loadLibrary("c++_shared")
        System.loadLibrary("appleMusicSDK")
    }

    override fun pause() { controller?.pause() }
    override fun resume() { controller?.play() }
    override fun seek(positionMs: Long) { controller?.seekToPosition(positionMs) }
    override fun positionMs(): Long = controller?.currentPosition?.coerceAtLeast(0) ?: 0

    /** Stops the music; the player stays, ready for the next song. */
    override fun stop() {
        generation++
        request = null
        controller?.stop()
    }

    private fun player(): MediaPlayerController {
        if (authorizationLost) release()
        controller?.let { return it }
        val player = MediaPlayerControllerFactory.createLocalController(context, Handler(Looper.getMainLooper()), tokens)
        player.addListener(listener)
        controller = player
        if (refreshJob == null) refreshJob = scope.launch { keepDeveloperTokenFresh() }
        return player
    }

    private fun release() {
        refreshJob?.cancel()
        refreshJob = null
        controller?.let { player ->
            player.removeListener(listener)
            player.stop()
            player.release()
        }
        controller = null
        authorizationLost = false
    }

    /** Fetches the developer token when the one at hand is missing or close to its hour. */
    private suspend fun refreshDeveloperToken() {
        val fetched = tokenAt
        if (developerToken.isNotEmpty() && fetched != null && fetched.elapsedNow() < TOKEN_LIFETIME) return
        developerToken = api.appleMusicDeveloperToken().token
        tokenAt = TimeSource.Monotonic.markNow()
    }

    /** Renews the token before it expires: the SDK asks for it from its own threads and cannot wait. */
    private suspend fun keepDeveloperTokenFresh() {
        while (true) {
            val age = tokenAt?.elapsedNow()
            delay(if (age == null) TOKEN_LIFETIME else (TOKEN_LIFETIME - age).coerceAtLeast(RETRY_DELAY))
            try {
                developerToken = api.appleMusicDeveloperToken().token
                tokenAt = TimeSource.Monotonic.markNow()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // The current token still has some time left: try again shortly.
            }
        }
    }

    private val listener = object : MediaPlayerController.Listener {
        override fun onPlayerStateRestored(controller: MediaPlayerController) = Unit
        override fun onPlaybackStateChanged(controller: MediaPlayerController, previousState: Int, currentState: Int) = Unit
        override fun onPlaybackStateUpdated(controller: MediaPlayerController) = Unit
        override fun onBufferingStateChanged(controller: MediaPlayerController, isBuffering: Boolean) = Unit
        override fun onCurrentItemChanged(controller: MediaPlayerController, previousItem: PlayerQueueItem?, currentItem: PlayerQueueItem?) = Unit
        override fun onItemEnded(controller: MediaPlayerController, queueItem: PlayerQueueItem, position: Long) {
            current()?.onEnded?.invoke()
        }
        override fun onMetadataUpdated(controller: MediaPlayerController, item: PlayerQueueItem?) = Unit
        override fun onPlaybackQueueChanged(controller: MediaPlayerController, items: MutableList<PlayerQueueItem>) = Unit
        override fun onPlaybackQueueItemsAdded(controller: MediaPlayerController, items: PlaybackQueueItemsAdded) = Unit
        override fun onPlaybackError(controller: MediaPlayerController, error: MediaPlayerException) {
            val target = current() ?: return
            if (error.type == MediaPlayerException.TYPE_MUSIC_USER_TOKEN_ERROR) {
                connection.disconnect()
                // Released on the next song: the SDK is still inside this callback.
                authorizationLost = true
                target.onError(IllegalStateException("Apple Music authorization expired. Connect Apple Music again.", error))
            } else {
                target.onError(error)
            }
        }
        override fun onPlaybackModeChanged(controller: MediaPlayerController, mode: PlaybackModeContainer) = Unit
        override fun onPlaybackRepeatModeChanged(controller: MediaPlayerController, mode: Int) = Unit
        override fun onPlaybackShuffleModeChanged(controller: MediaPlayerController, mode: Int) = Unit
    }

    /** The song being played now, or null when the callbacks belong to a song already replaced or stopped. */
    private fun current(): Request? = request?.takeIf { it.generation == generation }

    private class Request(val generation: Long, val onEnded: () -> Unit, val onError: (Exception) -> Unit)

    private companion object {
        /** The server signs developer tokens for an hour; this leaves room before the SDK needs a new one. */
        val TOKEN_LIFETIME = 45.minutes
        val RETRY_DELAY = 1.minutes
    }
}
