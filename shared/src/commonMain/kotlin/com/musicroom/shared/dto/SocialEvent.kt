package com.musicroom.shared.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Live events the server pushes on the social WebSocket. */
@Serializable
sealed interface SocialEvent {
    /** A message the user sent (from another device) or received. */
    @Serializable
    @SerialName("message")
    data class Message(val message: MessageResponse) : SocialEvent

    /** A friend started or stopped typing to the user. */
    @Serializable
    @SerialName("typing")
    data class Typing(val userId: String, val typing: Boolean) : SocialEvent

    /** The user's messages to [userId] were read. */
    @Serializable
    @SerialName("read")
    data class Read(val userId: String) : SocialEvent

    @Serializable
    @SerialName("notification")
    data class Notification(val notification: NotificationResponse) : SocialEvent

    /** The friends list or the requests changed. */
    @Serializable
    @SerialName("friends")
    data object FriendsChanged : SocialEvent

    @Serializable
    @SerialName("counts")
    data class Counts(val counts: SocialCountsResponse) : SocialEvent
}
