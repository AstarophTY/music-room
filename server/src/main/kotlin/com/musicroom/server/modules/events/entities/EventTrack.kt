package com.musicroom.server.modules.events.entities

import com.musicroom.shared.dto.TrackResponse
import java.time.OffsetDateTime
import java.util.UUID

/** A row of [EventTracksTable]. [status] is one of the EventTracksTable constants. */
data class EventTrack(
    val id: UUID,
    val eventId: UUID,
    val track: TrackResponse,
    val proposedBy: UUID?,
    val votes: Int,
    val status: String,
    val createdAt: OffsetDateTime,
)
