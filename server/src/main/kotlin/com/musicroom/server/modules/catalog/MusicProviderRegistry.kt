package com.musicroom.server.modules.catalog

import com.musicroom.server.modules.applemusic.AppleMusicCatalog
import com.musicroom.server.modules.events.EventPlaybackProviders
import com.musicroom.server.modules.spotify.SpotifyService
import com.musicroom.shared.dto.PlayRequest
import com.musicroom.shared.dto.TrackResponse
import com.musicroom.shared.dto.TrackSource
import java.util.UUID

/** Server-side catalog and remote playback capabilities for configured providers. */
class MusicProviderRegistry(spotify: SpotifyService, appleMusic: AppleMusicCatalog) {
    private val spotifyProvider = Spotify(spotify)
    val catalog = TrackCatalog(listOf(AppleMusic(appleMusic), spotifyProvider))
    val eventPlayback = EventPlaybackProviders(listOf(spotifyProvider))

    private class AppleMusic(private val catalog: AppleMusicCatalog) : TrackCatalog.Provider {
        override fun accepts(id: String) = TrackSource.fromId(id) == TrackSource.APPLE_MUSIC
        override suspend fun track(userId: UUID, id: String) = catalog.track(id)
    }

    private class Spotify(private val service: SpotifyService) : TrackCatalog.Provider, EventPlaybackProviders.Provider {
        override val source = TrackSource.SPOTIFY
        override fun accepts(id: String) = true
        override suspend fun track(userId: UUID, id: String) = service.track(userId, id)
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
