package com.musicroom.app.player

/** Controls audio played on the current device. */
interface DevicePlaybackSession {
    val canResume: Boolean get() = true
    fun pause()
    fun resume()
    fun seek(positionMs: Long)
    fun positionMs(): Long
    fun stop()
    fun pauseAtEnd() {
        pause()
        seek(0)
    }
}
