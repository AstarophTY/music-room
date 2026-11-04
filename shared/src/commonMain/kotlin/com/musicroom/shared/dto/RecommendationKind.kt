package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Why a section is recommended: the user's own top tracks, or more from one of their top artists. */
@Serializable
enum class RecommendationKind { TOP_TRACKS, ARTIST }
