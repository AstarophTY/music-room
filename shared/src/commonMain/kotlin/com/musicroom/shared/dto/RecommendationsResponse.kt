package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Recommendations built from the user's Spotify listening (top tracks and top artists). */
@Serializable
data class RecommendationsResponse(
    val sections: List<RecommendationSection>,
)
