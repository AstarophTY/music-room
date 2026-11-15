package com.musicroom.app.player

import com.musicroom.app.common.UiText
import com.musicroom.shared.dto.TrackResponse
import com.musicroom.shared.dto.TrackSource

/** Controls audio played on the current device. */
interface DevicePlaybackSession {
    val source: TrackSource
    fun availabilityError(): UiText? = null
    fun connectionError(): UiText? = null
    fun start(track: TrackResponse)
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
