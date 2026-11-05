package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** [location]: the user's position, needed with [EventVoteLicense.LOCATION]. */
@Serializable
data class EventVoteRequest(val location: GeoPoint? = null)
