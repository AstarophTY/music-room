package com.musicroom.app.social

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.ConversationResponse
import com.musicroom.shared.dto.FriendsResponse
import com.musicroom.shared.dto.FriendshipStatus
import com.musicroom.shared.dto.NotificationResponse
import com.musicroom.shared.dto.PublicUserResponse
import com.musicroom.shared.dto.SocialClientEvent
import com.musicroom.shared.dto.SocialCountsResponse
import com.musicroom.shared.dto.SocialEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/**
 * Friends, chats and notifications of the logged-in user, kept up to date by the live social connection
 * ([run]). [meId] is the user's own id. Screens read the state and listen to [events] (new messages, read receipts) while they show.
 */
class SocialController(private val api: MusicRoomApi, private val meId: String, private val scope: CoroutineScope) {
    /** What waits for the user: shown as numbers on the bell and the chat icon. */
    var counts by mutableStateOf(SocialCountsResponse())
        private set

    /** Null until loaded. */
    var friends by mutableStateOf<FriendsResponse?>(null)
        private set

    /** Chats with each friend, the most recent first. Null until loaded. */
    var conversations by mutableStateOf<List<ConversationResponse>?>(null)
        private set

    /** Newest first. Null until loaded. */
    var notifications by mutableStateOf<List<NotificationResponse>?>(null)
        private set

    /** Friends typing to the user right now, by id. */
    val typing = mutableStateMapOf<String, Boolean>()

    /** The chat on screen: its messages are marked read as they arrive. */
    var openChat: String? = null

    /** A notification that just arrived, for a short banner; cleared by [dismissLatest]. */
    var latest by mutableStateOf<NotificationResponse?>(null)
        private set

    private val _events = MutableSharedFlow<SocialEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<SocialEvent> = _events

    // Only the last typing state matters: older ones are dropped while offline.
    private val outgoing = Channel<SocialClientEvent>(Channel.CONFLATED)
    private val typingTimeouts = mutableMapOf<String, Job>()

    /** Keeps the live connection open, reconnecting with a growing pause. Runs until cancelled (logout). */
    suspend fun run() {
        var pause = MIN_RETRY_MS
        while (true) {
            try {
                api.socialEvents(
                    outgoing,
                    onOpen = {
                        pause = MIN_RETRY_MS
                        // Whatever happened while offline.
                        scope.launch { refreshAll() }
                    },
                    onEvent = ::handle,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Offline or server restarting: try again later.
            }
            delay(pause)
            pause = (pause * 2).coerceAtMost(MAX_RETRY_MS)
        }
    }

    suspend fun refreshAll() {
        quietly { counts = api.socialCounts() }
        quietly { friends = api.friends() }
        quietly { conversations = api.conversations().items }
        quietly { notifications = api.notifications().items }
    }

    suspend fun refreshFriends() {
        friends = api.friends()
    }

    suspend fun refreshConversations() {
        conversations = api.conversations().items
    }

    suspend fun refreshNotifications() {
        notifications = api.notifications().items
    }

    /** Asks [userId] to be friends, or accepts their request. Returns them with the new status. */
    suspend fun addFriend(userId: String): PublicUserResponse {
        val user = api.addFriend(userId)
        afterFriendChange(userId, user.friendship)
        return user
    }

    /** Removes a friend, takes back a request, or declines one. */
    suspend fun removeFriend(userId: String) {
        api.removeFriend(userId)
        afterFriendChange(userId, FriendshipStatus.NONE)
    }

    /** Tells the friend [userId] the user is typing (sent again every few seconds by the chat) or stopped. */
    fun sendTyping(userId: String, typing: Boolean) {
        outgoing.trySend(SocialClientEvent.Typing(userId, typing))
    }

    /** The user saw the notifications: the bell number goes away. */
    fun markNotificationsRead() {
        if (counts.unreadNotifications == 0) return
        counts = counts.copy(unreadNotifications = 0)
        notifications = notifications?.map { it.copy(read = true) }
        scope.launch { quietly { api.markNotificationsRead() } }
    }

    /** The user opened the chat with [userId]: their messages are read. */
    fun markChatRead(userId: String) {
        val unread = conversations?.firstOrNull { it.friend.id == userId }?.unreadCount ?: 0
        conversations = conversations?.map { if (it.friend.id == userId) it.copy(unreadCount = 0) else it }
        if (unread > 0) counts = counts.copy(unreadMessages = (counts.unreadMessages - unread).coerceAtLeast(0))
        scope.launch { quietly { api.markConversationRead(userId) } }
    }

    fun dismissLatest() {
        latest = null
    }

    /** The friend [userId] in the user's lists, wherever they are (friends or requests). */
    fun person(userId: String): PublicUserResponse? = friends?.let { list ->
        (list.friends + list.received + list.sent).firstOrNull { it.id == userId }
    } ?: conversations?.firstOrNull { it.friend.id == userId }?.friend

    private suspend fun handle(event: SocialEvent) {
        when (event) {
            is SocialEvent.Counts -> counts = event.counts
            is SocialEvent.Message -> {
                val message = event.message
                val friendId = if (message.senderId == meId) message.recipientId else message.senderId
                val incoming = message.senderId == friendId
                if (incoming) stopTyping(friendId)
                val list = conversations
                if (list == null || list.none { it.friend.id == friendId }) {
                    quietly { refreshConversations() }
                } else {
                    val updated = list.map {
                        if (it.friend.id != friendId) {
                            it
                        } else {
                            it.copy(
                                lastMessage = message,
                                unreadCount = if (incoming && openChat != friendId) it.unreadCount + 1 else it.unreadCount,
                            )
                        }
                    }
                    conversations = updated.sortedByDescending { it.lastMessage?.sentAt ?: Long.MIN_VALUE }
                }
                if (incoming && openChat == friendId) markChatRead(friendId)
            }
            is SocialEvent.Typing -> if (event.typing) startTyping(event.userId) else stopTyping(event.userId)
            is SocialEvent.Read -> conversations = conversations?.map { conversation ->
                val last = conversation.lastMessage
                if (conversation.friend.id == event.userId && last != null && last.recipientId == event.userId) {
                    conversation.copy(lastMessage = last.copy(read = true))
                } else {
                    conversation
                }
            }
            is SocialEvent.Notification -> {
                notifications = listOf(event.notification) + notifications.orEmpty().filter { it.id != event.notification.id }
                latest = event.notification
            }
            SocialEvent.FriendsChanged -> {
                quietly { refreshFriends() }
                quietly { refreshConversations() }
            }
        }
        _events.emit(event)
    }

    private fun startTyping(userId: String) {
        typing[userId] = true
        typingTimeouts.remove(userId)?.cancel()
        // The friend's app sends it again while they type: without news, they stopped.
        typingTimeouts[userId] = scope.launch {
            delay(TYPING_TIMEOUT_MS)
            typing.remove(userId)
            typingTimeouts.remove(userId)
        }
    }

    private fun stopTyping(userId: String) {
        typing.remove(userId)
        typingTimeouts.remove(userId)?.cancel()
    }

    private suspend fun afterFriendChange(userId: String, status: FriendshipStatus) {
        // The notification of a request answered here is no longer actionable.
        notifications = notifications?.map { item ->
            val actor = item.actor
            if (actor?.id == userId) item.copy(actor = actor.copy(friendship = status)) else item
        }
        quietly { refreshFriends() }
        quietly { refreshConversations() }
        quietly { counts = api.socialCounts() }
    }

    private suspend fun quietly(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Shown again on the next refresh; the screens show their own errors.
        }
    }

    private companion object {
        const val MIN_RETRY_MS = 1_000L
        const val MAX_RETRY_MS = 30_000L
        const val TYPING_TIMEOUT_MS = 6_000L
    }
}
