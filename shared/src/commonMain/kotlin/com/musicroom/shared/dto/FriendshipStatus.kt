package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Link between the user who asks and another user. */
@Serializable
enum class FriendshipStatus {
    NONE,
    FRIENDS,
    /** The user asked the other one, who has not answered yet. */
    REQUEST_SENT,
    /** The other one asked the user. */
    REQUEST_RECEIVED,
    /** It is the user themself. */
    SELF,
}
