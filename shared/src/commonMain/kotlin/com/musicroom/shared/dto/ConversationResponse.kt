package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** The chat with a friend: their last message and how many the user has not read. */
@Serializable
data class ConversationResponse(
    val friend: PublicUserResponse,
    val lastMessage: MessageResponse? = null,
    val unreadCount: Int = 0,
)
