package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** The user's own profile details: [publicInfo] for everyone, [friendsInfo] for friends, [privateInfo] for them only. */
@Serializable
data class UserDetailsResponse(
    val publicInfo: String = "",
    val friendsInfo: String = "",
    val privateInfo: String = "",
    val musicPreferences: List<String> = emptyList(),
)
