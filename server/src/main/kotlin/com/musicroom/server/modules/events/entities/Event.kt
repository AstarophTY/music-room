package com.musicroom.server.modules.events.entities

import com.musicroom.shared.dto.EventStatus
import com.musicroom.shared.dto.EventVisibility
import com.musicroom.shared.dto.EventVoteLicense
import com.musicroom.shared.dto.GeoPoint
import java.time.OffsetDateTime
import java.util.UUID

/** A row of [EventsTable]. */
data class Event(
    val id: UUID,
    val ownerId: UUID,
    val name: String,
    val description: String,
    val visibility: EventVisibility,
    val voteLicense: EventVoteLicense,
    val location: GeoPoint?,
    val radiusMeters: Int?,
    val startsAt: OffsetDateTime?,
    val endsAt: OffsetDateTime?,
    val status: EventStatus,
)
