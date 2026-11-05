package com.musicroom.shared.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Events the app sends on the social WebSocket. */
@Serializable
sealed interface SocialClientEvent {
    /** The user is typing to [userId] (sent again every few seconds), or stopped. */
    @Serializable
    @SerialName("typing")
    data class Typing(val userId: String, val typing: Boolean) : SocialClientEvent
}
