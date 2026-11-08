package com.musicroom.app.player

import com.musicroom.app.music.AppleMusicPlayback
import com.musicroom.shared.MusicRoomApi
import kotlinx.coroutines.CoroutineScope

/** Wires platform and remote playback adapters at the application boundary. */
class PlaybackEnvironment(
    val remote: RemotePlaybackProvider,
    private val createDevices: (onEnded: () -> Unit, onError: (Exception) -> Unit) -> DevicePlaybackProviders,
) {
    fun devices(onEnded: () -> Unit, onError: (Exception) -> Unit) = createDevices(onEnded, onError)

    companion object {
        fun create(api: MusicRoomApi, scope: CoroutineScope, apple: AppleMusicPlayback, remote: RemotePlaybackProvider = SpotifyPlaybackProvider(api)) =
            PlaybackEnvironment(remote) { onEnded, onError -> DevicePlaybackProviders.create(api, scope, apple, onEnded, onError) }
    }
}
