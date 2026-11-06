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

class AndroidAppleMusicPlayback(
    private val context: Context,
    private val connection: AndroidAppleMusicConnection,
    private val api: MusicRoomApi,
) : AppleMusicPlayback {
    override val available = true
    override val connected get() = connection.connected
    private var controller: MediaPlayerController? = null
    private var listener: MediaPlayerController.Listener? = null

    override suspend fun play(songId: String, onEnded: () -> Unit, onError: (Exception) -> Unit) {
        try {
            loadSdk()
        } catch (e: UnsatisfiedLinkError) {
            throw IllegalStateException("Apple Music playback is not supported on this device", e)
        }
        val userToken = connection.musicUserToken() ?: error("Connect Apple Music before playback")
        val developerToken = api.appleMusicDeveloperToken().token
        stop()
        val tokens = object : TokenProvider {
            override fun getDeveloperToken(): String = developerToken
            override fun getUserToken(): String = userToken
            override fun getAppleMusicStoreFrontId(): String? = connection.appleStorefront()
        }
        val player = MediaPlayerControllerFactory.createLocalController(context, Handler(Looper.getMainLooper()), tokens)
        val callbacks = object : MediaPlayerController.Listener {
            override fun onPlayerStateRestored(controller: MediaPlayerController) = Unit
            override fun onPlaybackStateChanged(controller: MediaPlayerController, previousState: Int, currentState: Int) = Unit
            override fun onPlaybackStateUpdated(controller: MediaPlayerController) = Unit
            override fun onBufferingStateChanged(controller: MediaPlayerController, isBuffering: Boolean) = Unit
            override fun onCurrentItemChanged(controller: MediaPlayerController, previousItem: PlayerQueueItem?, currentItem: PlayerQueueItem?) = Unit
            override fun onItemEnded(controller: MediaPlayerController, queueItem: PlayerQueueItem, position: Long) = onEnded()
            override fun onMetadataUpdated(controller: MediaPlayerController, item: PlayerQueueItem?) = Unit
            override fun onPlaybackQueueChanged(controller: MediaPlayerController, items: MutableList<PlayerQueueItem>) = Unit
            override fun onPlaybackQueueItemsAdded(controller: MediaPlayerController, items: PlaybackQueueItemsAdded) = Unit
            override fun onPlaybackError(controller: MediaPlayerController, error: MediaPlayerException) = onError(error)
            override fun onPlaybackModeChanged(controller: MediaPlayerController, mode: PlaybackModeContainer) = Unit
            override fun onPlaybackRepeatModeChanged(controller: MediaPlayerController, mode: Int) = Unit
            override fun onPlaybackShuffleModeChanged(controller: MediaPlayerController, mode: Int) = Unit
        }
        controller = player
        listener = callbacks
        player.addListener(callbacks)
        player.prepare(CatalogPlaybackQueueItemProvider.Builder().items(MediaItemType.SONG, songId).build(), true)
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
        controller?.let { player ->
            listener?.let(player::removeListener)
            player.stop()
            player.release()
        }
        listener = null
        controller = null
    }
}
