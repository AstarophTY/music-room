package com.musicroom.server.modules.events

import com.musicroom.server.modules.spotify.SpotifyService
import com.musicroom.shared.dto.PlayRequest
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

    companion object {
        fun create(spotify: SpotifyService) = EventPlaybackProviders(listOf(Spotify(spotify)))
    }

    private class Spotify(private val service: SpotifyService) : Provider {
        override val source = TrackSource.SPOTIFY
        override suspend fun available(userId: UUID): Boolean {
            val status = service.status(userId)
            return status.configured && status.connected && status.product == "premium"
        }
        override suspend fun state(userId: UUID) = service.playbackState(userId)
        override suspend fun play(userId: UUID, track: TrackResponse?) {
            service.play(userId, if (track == null) PlayRequest() else PlayRequest(uris = listOf(track.uri)))
        }
        override suspend fun pause(userId: UUID) { service.pause(userId) }
        override suspend fun enqueue(userId: UUID, track: TrackResponse) { service.addToQueue(userId, listOf(track.uri)) }
    }
}
