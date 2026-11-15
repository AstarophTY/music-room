package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Proposes a catalog track; [location] is required for location-restricted voting. */
@Serializable
data class ProposeTrackRequest(val trackId: String, val location: GeoPoint? = null, val catalogId: CatalogId? = null)
