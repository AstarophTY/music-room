package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** What waits for the user: shown as a number on their avatar. */
@Serializable
data class SocialCountsResponse(
    val unreadNotifications: Int = 0,
    val unreadMessages: Int = 0,
    val pendingRequests: Int = 0,
)
