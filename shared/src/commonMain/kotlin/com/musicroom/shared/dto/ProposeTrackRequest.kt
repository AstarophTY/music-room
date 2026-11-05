package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Proposes the Spotify track [trackId]. [location]: the user's position, needed with [EventVoteLicense.LOCATION]. */
@Serializable
data class ProposeTrackRequest(val trackId: String, val location: GeoPoint? = null)
