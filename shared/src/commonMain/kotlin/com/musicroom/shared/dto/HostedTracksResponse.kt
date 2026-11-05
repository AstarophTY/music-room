package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Songs uploaded to Music Room by a user, newest first. */
@Serializable
data class HostedTracksResponse(val items: List<TrackResponse>)
