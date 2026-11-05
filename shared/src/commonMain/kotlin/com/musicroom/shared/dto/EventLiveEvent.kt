package com.musicroom.shared.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** What the event WebSocket pushes to the people following an event. */
@Serializable
sealed interface EventLiveEvent {
    /** The event changed (a song proposed, a vote, the next song...): its new state, as this user sees it. */
    @Serializable
    @SerialName("state")
    data class State(val detail: EventDetailResponse) : EventLiveEvent

    /** The owner deleted the event. */
    @Serializable
    @SerialName("deleted")
    data object Deleted : EventLiveEvent
}
