package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** The user's friends (by name), the requests they received and the ones they sent (newest first). */
@Serializable
data class FriendsResponse(
    val friends: List<PublicUserResponse> = emptyList(),
    val received: List<PublicUserResponse> = emptyList(),
    val sent: List<PublicUserResponse> = emptyList(),
)
