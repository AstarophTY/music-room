package com.musicroom.app.player

import com.musicroom.shared.dto.PlaybackStateResponse

interface PlaybackProvider {
    suspend fun playbackState(): PlaybackStateResponse
    suspend fun play(uris: List<String> = emptyList(), index: Int = 0, contextUri: String? = null)
    suspend fun pause()
    suspend fun skipNext()
    suspend fun skipPrevious()
    suspend fun seek(positionMs: Long)
}
