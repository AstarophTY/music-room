package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Chats with each friend, the most recent first (friends never written to come last). */
@Serializable
data class ConversationsResponse(val items: List<ConversationResponse> = emptyList())
