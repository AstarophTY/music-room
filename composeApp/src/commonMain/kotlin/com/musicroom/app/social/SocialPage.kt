package com.musicroom.app.social

/**
 * Pages of the social part, opened over the tabs: friends, notifications, chats, a chat with a friend and
 * another user's profile. [key] is saved across configuration changes and read back with [parse].
 */
sealed interface SocialPage {
    val key: String

    data object Friends : SocialPage {
        override val key = "friends"
    }

    data object Notifications : SocialPage {
        override val key = "notifications"
    }

    data object Conversations : SocialPage {
        override val key = "conversations"
    }

    data class Chat(val userId: String) : SocialPage {
        override val key = "chat|$userId"
    }

    data class Profile(val userId: String) : SocialPage {
        override val key = "profile|$userId"
    }

    companion object {
        fun parse(key: String): SocialPage? {
            val kind = key.substringBefore('|')
            val id = key.substringAfter('|', "")
            return when (kind) {
                Friends.key -> Friends
                Notifications.key -> Notifications
                Conversations.key -> Conversations
                "chat" -> Chat(id)
                "profile" -> Profile(id)
                else -> null
            }
        }
    }
}
