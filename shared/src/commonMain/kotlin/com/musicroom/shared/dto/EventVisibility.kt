package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Who sees an event: everyone (listed in the public events), or only its owner and the people invited. */
@Serializable
enum class EventVisibility { PUBLIC, PRIVATE }
