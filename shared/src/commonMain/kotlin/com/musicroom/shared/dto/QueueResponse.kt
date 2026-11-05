package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** The next tracks Spotify will play after the current one, in order. */
@Serializable
data class QueueResponse(val tracks: List<TrackResponse> = emptyList())
