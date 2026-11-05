package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** A chat message between two friends. [sentAt]: epoch milliseconds. [read]: the recipient has seen it. */
@Serializable
data class MessageResponse(
    val id: String,
    val senderId: String,
    val recipientId: String,
    val body: String,
    val sentAt: Long,
    val read: Boolean = false,
)
