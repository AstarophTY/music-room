package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/**
 * Creates an event, or changes its settings. With [EventVoteLicense.LOCATION], [location] and [radiusMeters] are
 * required; [startsAt] and [endsAt] (epoch ms, optional) limit the hours when people can vote.
 */
@Serializable
data class CreateEventRequest(
    val name: String,
    val description: String = "",
    val visibility: EventVisibility = EventVisibility.PUBLIC,
    val voteLicense: EventVoteLicense = EventVoteLicense.EVERYONE,
    val location: GeoPoint? = null,
    val radiusMeters: Int? = null,
    val startsAt: Long? = null,
    val endsAt: Long? = null,
)
