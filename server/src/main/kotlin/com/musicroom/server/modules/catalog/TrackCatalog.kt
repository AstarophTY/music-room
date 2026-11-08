package com.musicroom.server.modules.catalog

import com.musicroom.server.modules.applemusic.AppleMusicCatalog
import com.musicroom.server.modules.spotify.SpotifyService
import com.musicroom.shared.dto.TrackSource
import com.musicroom.shared.dto.TrackResponse
import java.util.UUID

/** Resolves catalog identifiers through the provider that owns them. */
class TrackCatalog(private val providers: List<Provider>) {
    interface Provider {
        fun accepts(id: String): Boolean
        suspend fun track(userId: UUID, id: String): TrackResponse
    }

    suspend fun track(userId: UUID, id: String): TrackResponse =
        providers.first { it.accepts(id) }.track(userId, id)

    companion object {
        fun create(spotify: SpotifyService, appleMusic: AppleMusicCatalog) = TrackCatalog(
            listOf(AppleMusic(appleMusic), Spotify(spotify)),
        )
    }

    private class AppleMusic(private val catalog: AppleMusicCatalog) : Provider {
        override fun accepts(id: String) = TrackSource.fromId(id) == TrackSource.APPLE_MUSIC
        override suspend fun track(userId: UUID, id: String) = catalog.track(id)
    }

    private class Spotify(private val service: SpotifyService) : Provider {
        override fun accepts(id: String) = true
        override suspend fun track(userId: UUID, id: String) = service.track(userId, id)
    }
}
