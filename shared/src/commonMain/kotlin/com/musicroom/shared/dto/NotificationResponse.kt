package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Something that happened to the user. [createdAt]: epoch milliseconds. */
@Serializable
data class NotificationResponse(
    val id: String,
    val type: NotificationType,
    val actor: PublicUserResponse? = null,
    val playlistId: String? = null,
    val playlistName: String? = null,
    val createdAt: Long,
    val read: Boolean = false,
)
