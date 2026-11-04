package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** A row of recommended tracks. [artist] is the seed artist when [kind] is ARTIST. */
@Serializable
data class RecommendationSection(
    val kind: RecommendationKind,
    val tracks: List<TrackResponse>,
    val artist: String? = null,
)
