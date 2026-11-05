package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Which events to list. */
@Serializable
enum class EventsScope {
    /** Public events, those playing first. */
    PUBLIC,

    /** Public events that have an area, the closest to the position sent first. */
    NEARBY,

    /** Events the user was invited to. */
    INVITED,

    /** The user's own events. */
    MINE,
}
