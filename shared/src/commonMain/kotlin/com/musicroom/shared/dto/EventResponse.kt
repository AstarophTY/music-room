package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/**
 * An event (a room where people propose songs and vote for the next one), as seen by the user who asks.
 * [location], [radiusMeters], [startsAt] and [endsAt] (epoch ms) limit who votes with [EventVoteLicense.LOCATION].
 */
@Serializable
data class EventResponse(
    val id: String,
    val name: String,
    val description: String = "",
    val visibility: EventVisibility,
    val voteLicense: EventVoteLicense,
    val status: EventStatus,
    val owner: PublicUserResponse,
    val isOwner: Boolean,
    /** The user was invited. */
    val isInvited: Boolean,
    /**
     * The user can propose and vote. With [EventVoteLicense.LOCATION], true when the hours allow it: their position
     * is checked on each vote.
     */
    val canVote: Boolean,
    val location: GeoPoint? = null,
    val radiusMeters: Int? = null,
    val startsAt: Long? = null,
    val endsAt: Long? = null,
    /** Songs waiting in the queue. */
    val queuedCount: Int = 0,
    /** People following the event right now. */
    val listeners: Int = 0,
    val nowPlaying: TrackResponse? = null,
)
