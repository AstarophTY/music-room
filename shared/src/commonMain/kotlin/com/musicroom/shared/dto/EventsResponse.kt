package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

@Serializable
data class EventsResponse(val items: List<EventResponse> = emptyList())
