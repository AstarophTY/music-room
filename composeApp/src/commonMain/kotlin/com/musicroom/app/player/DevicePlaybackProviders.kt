package com.musicroom.app.player

import com.musicroom.app.music.AppleMusicPlayback
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.TrackSource
import kotlinx.coroutines.CoroutineScope

/** Constructs and selects device-local playback implementations. */
class DevicePlaybackProviders(private val sessions: List<DevicePlaybackSession>) {
    fun find(source: TrackSource): DevicePlaybackSession? = sessions.firstOrNull { it.source == source }
    val all: List<DevicePlaybackSession> get() = sessions

    companion object {
        fun create(api: MusicRoomApi, scope: CoroutineScope, apple: AppleMusicPlayback, onEnded: () -> Unit, onError: (Exception) -> Unit) =
            DevicePlaybackProviders(listOf(
                HostedPlayback(api, scope, onEnded, onError),
                AppleMusicPlaybackSession(apple, scope, onEnded, onError),
            ))
    }
}
