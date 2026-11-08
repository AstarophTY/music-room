package com.musicroom.server.modules.events

import com.musicroom.shared.dto.PlaybackStateResponse
import com.musicroom.shared.dto.TrackResponse
import com.musicroom.shared.dto.TrackSource
import java.util.UUID

/** Remote playback capabilities available to an event host. */
class EventPlaybackProviders(private val providers: List<Provider>) {
    interface Provider {
        val source: TrackSource
        suspend fun available(userId: UUID): Boolean
        suspend fun state(userId: UUID): PlaybackStateResponse
        suspend fun play(userId: UUID, track: TrackResponse? = null)
        suspend fun pause(userId: UUID)
        suspend fun enqueue(userId: UUID, track: TrackResponse)
    }

    fun forTrack(track: TrackResponse?): Provider? = providers.firstOrNull { it.source == track?.source }

}
