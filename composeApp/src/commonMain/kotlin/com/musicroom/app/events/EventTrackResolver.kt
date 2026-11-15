package com.musicroom.app.events

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

}
