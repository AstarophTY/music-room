package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Whether the event's music plays on its owner's Spotify. */
@Serializable
enum class EventStatus {
    /** Not started yet, or stopped: songs can still be proposed and voted. */
    WAITING,

    /** The most voted songs play one after the other. */
    PLAYING,

    /** The owner paused the music. */
    PAUSED,
}
