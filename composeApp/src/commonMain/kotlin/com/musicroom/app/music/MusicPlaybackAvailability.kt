package com.musicroom.app.music

import com.musicroom.app.events.EventTrackResolver
import com.musicroom.shared.dto.TrackResponse
import com.musicroom.shared.dto.TrackSource

/** Playback capabilities available to the current listener. */
class MusicPlaybackAvailability(
    private val resolver: EventTrackResolver,
    private val playableSources: Set<TrackSource>,
    private val hostRemoteSources: Set<TrackSource>,
) {
    val canListen: Boolean get() = playableSources.isNotEmpty()
    fun hostControlsPlayback(track: TrackResponse): Boolean = track.source in hostRemoteSources
    suspend fun resolve(track: TrackResponse): TrackResponse? = resolver.resolve(track)
}
