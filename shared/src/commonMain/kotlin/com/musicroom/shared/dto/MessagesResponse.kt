package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** A page of a conversation, oldest first. [hasMore]: older messages exist before the first one. */
@Serializable
data class MessagesResponse(
    val items: List<MessageResponse> = emptyList(),
    val hasMore: Boolean = false,
)
