package com.musicroom.app.music

import com.musicroom.app.events.EventTrackResolver
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.TrackSource

/** Builds the capabilities offered by the user's connected music providers. */
class MusicProviderRegistry private constructor(private val providers: List<Provider>) {
    private interface Provider : MusicCatalogSearch.Provider, EventTrackResolver.Provider {
        val searchable: Boolean
    }

    fun catalogSearch() = MusicCatalogSearch(providers.filter { it.searchable })
    fun eventTrackResolver() = EventTrackResolver(providers)

    companion object {
        fun connected(api: MusicRoomApi, spotify: Boolean, appleConnected: Boolean, appleStorefront: String?): MusicProviderRegistry =
            MusicProviderRegistry(buildList {
                if (spotify) add(Spotify(api))
                if (appleConnected) add(AppleMusic(api, appleStorefront))
            })
    }

    private class Spotify(private val api: MusicRoomApi) : Provider {
        override val source = TrackSource.SPOTIFY
        override val searchable = true
        override suspend fun search(query: String, mood: String?) = api.search(query)
        override suspend fun tracks(query: String) = api.searchTracks(query).items
        override suspend fun suggestions() = api.myTopTracks().items
        override suspend fun find(isrc: String) = api.searchTracks("isrc:$isrc").items.firstOrNull { it.isrc.equals(isrc, ignoreCase = true) }
    }

    private class AppleMusic(private val api: MusicRoomApi, private val storefront: String?) : Provider {
        override val source = TrackSource.APPLE_MUSIC
        override val searchable get() = storefront != null
        override suspend fun search(query: String, mood: String?) = api.searchAppleMusic(mood ?: query, requireNotNull(storefront))
        override suspend fun suggestions() = api.appleMusicCharts(requireNotNull(storefront)).items
        override suspend fun find(isrc: String) = storefront?.let { region ->
            api.matchAppleMusic(isrc, region).items.firstOrNull { it.isrc.equals(isrc, ignoreCase = true) }
        }
    }
}
