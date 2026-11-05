package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Who can propose songs and vote in an event (the owner always can). */
@Serializable
enum class EventVoteLicense {
    /** Everyone who can see the event. */
    EVERYONE,

    /** Only the people invited. */
    INVITED,

    /** Only the people within the event's area, during its hours (the server checks the position sent). */
    LOCATION,
}
