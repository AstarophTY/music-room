package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Replaces all the user's profile details. */
@Serializable
data class UpdateUserDetailsRequest(
    val publicInfo: String = "",
    val friendsInfo: String = "",
    val privateInfo: String = "",
    val musicPreferences: List<String> = emptyList(),
)
