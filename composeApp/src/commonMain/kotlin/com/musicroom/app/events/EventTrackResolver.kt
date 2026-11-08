package com.musicroom.app.events

import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.TrackResponse
import com.musicroom.shared.dto.TrackSource

/** Finds a playable equivalent for an event track among the listener's enabled providers. */
class EventTrackResolver(private val providers: List<Provider>) {
    interface Provider {
        val source: TrackSource
        suspend fun find(isrc: String): TrackResponse?
    }

    suspend fun resolve(track: TrackResponse): TrackResponse? {
        if (track.source == TrackSource.MUSIC_ROOM || providers.any { it.source == track.source }) return track
        val isrc = track.isrc?.takeIf { it.matches(Regex("[A-Za-z0-9]{12}")) } ?: return null
        for (provider in providers) {
            val match = provider.find(isrc)
            if (match != null && match.isrc.equals(isrc, ignoreCase = true)) return match
        }
        return null
    }

    companion object {
        fun connected(api: MusicRoomApi, spotify: Boolean, appleConnected: Boolean, appleStorefront: String?): EventTrackResolver =
            EventTrackResolver(buildList {
                if (spotify) add(Spotify(api))
                if (appleConnected) add(AppleMusic(api, appleStorefront))
            })
    }

    private class Spotify(private val api: MusicRoomApi) : Provider {
        override val source = TrackSource.SPOTIFY
        override suspend fun find(isrc: String) =
            api.searchTracks("isrc:$isrc").items.firstOrNull { it.isrc.equals(isrc, ignoreCase = true) }
    }

    private class AppleMusic(private val api: MusicRoomApi, private val storefront: String?) : Provider {
        override val source = TrackSource.APPLE_MUSIC
        override suspend fun find(isrc: String) =
            storefront?.let { api.matchAppleMusic(isrc, it).items.firstOrNull { track -> track.isrc.equals(isrc, ignoreCase = true) } }
    }
}
