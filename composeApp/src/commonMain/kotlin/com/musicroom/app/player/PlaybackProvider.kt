package com.musicroom.app.player

import com.musicroom.shared.dto.DevicesResponse
import com.musicroom.shared.dto.PlaybackStateResponse
import com.musicroom.shared.dto.QueueResponse

interface PlaybackProvider {
    suspend fun playbackState(): PlaybackStateResponse
    suspend fun play(uris: List<String> = emptyList(), index: Int = 0, contextUri: String? = null)
    suspend fun pause()
    suspend fun skipNext()
    suspend fun skipPrevious()
    suspend fun seek(positionMs: Long)
    suspend fun devices(): DevicesResponse
    suspend fun transferPlayback(deviceId: String, play: Boolean)
    suspend fun queue(): QueueResponse
    suspend fun addToQueue(uris: List<String>)
}
