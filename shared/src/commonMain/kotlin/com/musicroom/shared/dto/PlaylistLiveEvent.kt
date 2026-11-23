package com.musicroom.shared.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** What the playlist WebSocket pushes to the people who have a playlist open. */
@Serializable
sealed interface PlaylistLiveEvent {
    /** The playlist changed: its new state, as this user sees it. */
    @Serializable
    @SerialName("state")
    data class State(val detail: PlaylistDetailResponse) : PlaylistLiveEvent

    /** The playlist was deleted, or this user can no longer see it. */
    @Serializable
    @SerialName("gone")
    data object Gone : PlaylistLiveEvent
}
