package com.musicroom.app.music

import androidx.compose.runtime.staticCompositionLocalOf

interface AppleMusicPlayback {
    /** Whether this platform can play Apple Music songs at all. */
    val available: Boolean

    /** Whether the user linked Apple Music, giving the tokens playback needs. */
    val connected: Boolean
    suspend fun play(songId: String, onEnded: () -> Unit, onError: (Exception) -> Unit)
    fun pause()
    fun resume()
    fun seek(positionMs: Long)
    fun positionMs(): Long
    fun stop()

    companion object {
        val Local = staticCompositionLocalOf<AppleMusicPlayback> { Unavailable }

        private object Unavailable : AppleMusicPlayback {
            override val available = false
            override val connected = false
            override suspend fun play(songId: String, onEnded: () -> Unit, onError: (Exception) -> Unit) = error("Apple Music playback is not available")
            override fun pause() = Unit
            override fun resume() = Unit
            override fun seek(positionMs: Long) = Unit
            override fun positionMs() = 0L
            override fun stop() = Unit
        }
    }
}
