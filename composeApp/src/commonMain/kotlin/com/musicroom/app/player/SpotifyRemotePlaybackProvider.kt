package com.musicroom.app.player

import com.musicroom.shared.dto.DevicesResponse
import com.musicroom.shared.dto.QueueResponse

interface SpotifyRemotePlaybackProvider : PlaybackProvider {
    suspend fun devices(): DevicesResponse
    suspend fun transferPlayback(deviceId: String, play: Boolean)
    suspend fun queue(): QueueResponse
    suspend fun addToQueue(uris: List<String>)
}
