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
import com.musicroom.app.music.AppleMusicException
import com.musicroom.app.music.AppleMusicPlayback
import com.musicroom.shared.MusicRoomApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

/**
 * Plays Apple Music songs on the device with the MusicKit SDK.
 *
 * Each song gets its own player: the SDK's `stop()` disposes the audio renderer for good, and preparing a new
 * queue on a stopped player decodes the song without ever asking for output buffers, so it plays silently.
 * The developer token the SDK asks for (at any moment, from its own threads) is fetched once and refreshed in
 * the background, so a song paused for longer than the token's hour still resumes. Songs started while a
 * previous one was still loading are dropped ([generation]), so the last requested song is the one heard.
 */
class AndroidAppleMusicPlayback(
    private val context: Context,
    private val connection: AndroidAppleMusicConnection,
    private val api: MusicRoomApi,
) : AppleMusicPlayback {
    override val available = true
    override val connected get() = connection.connected
    @Volatile private var controller: MediaPlayerController? = null
    private var generation = 0L
    private var request: Request? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * Building, preparing and releasing the SDK's player each take their time, and the caller is the main
     * thread: the app stopped answering taps long enough for Android to report it. One thread of its own
     * keeps them off it, in the order they were asked for.
     */
    private val sdk = Executors.newSingleThreadExecutor { task -> Thread(task, "apple-music-sdk") }.asCoroutineDispatcher()
    private var refreshJob: Job? = null
    @Volatile private var developerToken: String = ""
    private var tokenAt: TimeSource.Monotonic.ValueTimeMark? = null

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
        if (connection.musicUserToken().isNullOrBlank()) throw AppleMusicException("connect Apple Music first", whilePlaying = true)
        refreshDeveloperToken()
        currentCoroutineContext().ensureActive()
        // Another song was asked for while the token was fetched: that one wins.
        if (current != generation) return
        request = Request(current, onEnded, onError)
        if (refreshJob == null) refreshJob = scope.launch { keepDeveloperTokenFresh() }
        try {
            withContext(sdk) {
                loadSdk()
                player().prepare(CatalogPlaybackQueueItemProvider.Builder().items(MediaItemType.SONG, songId).build(), true)
            }
        } catch (e: UnsatisfiedLinkError) {
            if (current == generation) request = null
            throw AppleMusicException("its player does not run on this device", whilePlaying = true, cause = e)
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

    override fun stop() {
        generation++
        request = null
        val playing = controller ?: return
        controller = null
        scope.launch(sdk) { release(playing) }
    }

    /** A player for one song: a stopped one decodes without output buffers, so it would play in silence. */
    private fun player(): MediaPlayerController {
        controller?.let { release(it) }
        val player = MediaPlayerControllerFactory.createLocalController(context, Handler(Looper.getMainLooper()), tokens)
        player.addListener(listener)
        controller = player
        return player
    }

    /** Tearing a player down decodes its way out and blocks for seconds: never on the thread that asked. */
    private fun release(player: MediaPlayerController) {
        player.removeListener(listener)
        player.stop()
        player.release()
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
                target.onError(AppleMusicException("the authorization expired, connect Apple Music again", whilePlaying = true, cause = error))
            } else {
                target.onError(AppleMusicException(error.message ?: "error ${error.type}", whilePlaying = true, cause = error))
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
