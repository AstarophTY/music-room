package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** A song proposed in an event, with its votes and whether the user voted for it. */
@Serializable
data class EventTrackResponse(
    val id: String,
    val track: TrackResponse,
    /** Null when that account was deleted. */
    val proposedBy: PublicUserResponse? = null,
    val votes: Int,
    val voted: Boolean = false,
    val status: EventTrackStatus = EventTrackStatus.QUEUED,
)
