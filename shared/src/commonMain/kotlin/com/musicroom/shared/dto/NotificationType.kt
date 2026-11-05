package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

@Serializable
enum class NotificationType {
    /** [NotificationResponse.actor] asks to be friends. */
    FRIEND_REQUEST,
    /** [NotificationResponse.actor] accepted the user's request. */
    FRIEND_ACCEPTED,
    /** [NotificationResponse.actor] added the user to a playlist. */
    PLAYLIST_INVITE,
    /** [NotificationResponse.actor] invited the user to an event. */
    EVENT_INVITE,
}
