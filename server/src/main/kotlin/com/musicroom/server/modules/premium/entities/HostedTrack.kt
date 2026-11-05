package com.musicroom.server.modules.premium.entities

import com.musicroom.shared.dto.TrackResponse
import com.musicroom.shared.dto.TrackSource
import java.util.UUID

/** A song uploaded by [ownerId]. */
data class HostedTrack(val id: UUID, val ownerId: UUID, val title: String, val artist: String, val durationMs: Long) {
    fun toResponse() = TrackResponse(
        id = TrackResponse.HOSTED_PREFIX + id,
        name = title,
        artists = listOf(artist),
        album = "",
        durationMs = durationMs,
        uri = "musicroom:track:$id",
        source = TrackSource.MUSIC_ROOM,
    )
}
