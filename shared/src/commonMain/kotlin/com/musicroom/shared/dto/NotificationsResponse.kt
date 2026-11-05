package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** The latest notifications, newest first. */
@Serializable
data class NotificationsResponse(
    val items: List<NotificationResponse> = emptyList(),
    val unreadCount: Int = 0,
)
