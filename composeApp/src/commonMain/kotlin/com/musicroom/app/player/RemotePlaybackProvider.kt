package com.musicroom.app.player

import com.musicroom.app.common.UiText
import com.musicroom.shared.dto.DevicesResponse
import com.musicroom.shared.dto.QueueResponse
import com.musicroom.shared.dto.TrackSource

/** Playback capabilities provided by a remote music service. */
interface RemotePlaybackProvider : PlaybackProvider {
    val source: TrackSource
    /** Name of the service, shown in UI copy such as "Open %s". */
    val displayName: String
    /** Opens the service's app or web player, where its devices can be started; null when it has none. */
    val appUrl: String? get() = null
    fun notConnectedError(): UiText
    fun isDisconnected(error: Exception): Boolean = false
    fun isMissingDevice(error: Exception): Boolean = false
    /** Whether a remote account is currently linked and reachable. */
    suspend fun isConnected(): Boolean = false
    suspend fun devices(): DevicesResponse
    suspend fun transferPlayback(deviceId: String, play: Boolean)
    suspend fun queue(): QueueResponse
    suspend fun addToQueue(uris: List<String>)
}
