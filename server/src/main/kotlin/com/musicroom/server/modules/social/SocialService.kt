package com.musicroom.server.modules.social

import com.musicroom.server.common.exceptions.BadRequestException
import com.musicroom.server.common.exceptions.ForbiddenException
import com.musicroom.server.common.exceptions.NotFoundException
import com.musicroom.server.modules.events.EventFriends
import com.musicroom.server.modules.playlists.PlaylistFriends
import com.musicroom.server.modules.users.UserDetailsRepository
import com.musicroom.server.modules.users.UsersRepository
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.dto.ConversationResponse
import com.musicroom.shared.dto.ConversationsResponse
import com.musicroom.shared.dto.FriendsResponse
import com.musicroom.shared.dto.FriendshipStatus
import com.musicroom.shared.dto.MessageResponse
import com.musicroom.shared.dto.MessagesResponse
import com.musicroom.shared.dto.NotificationResponse
import com.musicroom.shared.dto.NotificationType
import com.musicroom.shared.dto.NotificationsResponse
import com.musicroom.shared.dto.PublicUserResponse
import com.musicroom.shared.dto.SocialCountsResponse
import com.musicroom.shared.dto.SocialEvent
import com.musicroom.shared.dto.UserProfileResponse
import com.musicroom.shared.dto.UsersResponse
import java.time.Instant
import java.util.UUID

/**
 * Friends (requests, accepting, removing), one-to-one chat between friends, notifications, and the live
 * events pushed to connected devices through [hub].
 */
class SocialService(
    private val people: PeopleRepository,
    private val friendships: FriendshipsRepository,
    private val messages: MessagesRepository,
    private val notifications: NotificationsRepository,
    private val users: UsersRepository,
    private val details: UserDetailsRepository,
    private val hub: SocialHub,
) : PlaylistFriends, EventFriends {

    suspend fun search(me: UUID, query: String): UsersResponse {
        val text = query.trim().take(MAX_QUERY)
        if (text.isEmpty()) throw BadRequestException("Type a name to search")
        // Names are unique: the user with exactly this name comes first, the others by name.
        val found = people.search(text, excluding = me, limit = SEARCH_LIMIT).sortedByDescending { it.displayName.equals(text, ignoreCase = true) }
        return UsersResponse(withStatus(me, found))
    }

    suspend fun profile(me: UUID, userId: UUID): UserProfileResponse {
        val user = users.findById(userId) ?: throw NotFoundException("No such user", ApiErrors.USER_NOT_FOUND)
        val public = people.find(listOf(userId))[userId] ?: throw NotFoundException("No such user", ApiErrors.USER_NOT_FOUND)
        val friendship = status(me, userId)
        val about = details.find(userId)
        // Private details never leave GET /users/me/details.
        val isClose = friendship == FriendshipStatus.FRIENDS || friendship == FriendshipStatus.SELF
        return UserProfileResponse(
            user = public.copy(friendship = friendship),
            socialLinks = user.socialLinks,
            friendCount = friendships.links(userId).count { it.accepted },
            publicInfo = about.publicInfo,
            friendsInfo = about.friendsInfo.takeIf { isClose && it.isNotEmpty() },
            musicPreferences = about.musicPreferences,
        )
    }

    suspend fun friends(me: UUID): FriendsResponse {
        val links = friendships.links(me)
        val users = people.find(links.map { it.other(me) })
        fun list(filter: (FriendshipsRepository.Link) -> Boolean, status: FriendshipStatus) =
            links.filter(filter).mapNotNull { users[it.other(me)]?.copy(friendship = status) }
        return FriendsResponse(
            friends = list({ it.accepted }, FriendshipStatus.FRIENDS).sortedBy { it.displayName.lowercase() },
            received = list({ !it.accepted && it.addresseeId == me }, FriendshipStatus.REQUEST_RECEIVED),
            sent = list({ !it.accepted && it.requesterId == me }, FriendshipStatus.REQUEST_SENT),
        )
    }

    /** Asks [other] to be friends, or accepts when they already asked. Returns them with the new status. */
    suspend fun addFriend(me: UUID, other: UUID): PublicUserResponse {
        if (me == other) throw BadRequestException("You cannot add yourself")
        val user = people.find(listOf(other))[other] ?: throw NotFoundException("No such user", ApiErrors.USER_NOT_FOUND)
        val link = friendships.find(me, other)
        when {
            link == null -> if (friendships.request(me, other)) notify(other, NotificationType.FRIEND_REQUEST, me)
            !link.accepted && link.requesterId == other -> if (friendships.accept(other, me)) {
                notifications.markRequestAnswered(me, other)
                notify(other, NotificationType.FRIEND_ACCEPTED, me)
            }
            else -> Unit // Already friends, or already asked.
        }
        friendsChanged(me, other)
        return user.copy(friendship = status(me, other))
    }

    /** Removes a friend, takes back a request, or declines one. */
    suspend fun removeFriend(me: UUID, other: UUID) {
        val link = friendships.find(me, other) ?: return
        friendships.delete(me, other)
        if (!link.accepted) notifications.delete(link.addresseeId, NotificationType.FRIEND_REQUEST, link.requesterId)
        friendsChanged(me, other)
        pushCounts(link.addresseeId)
    }

    suspend fun conversations(me: UUID): ConversationsResponse {
        val friends = friends(me).friends
        val unread = messages.unreadBySender(me)
        val items = friends.map { friend ->
            val id = UUID.fromString(friend.id)
            ConversationResponse(friend, messages.last(me, id), unread[id] ?: 0)
        }
        return ConversationsResponse(items.sortedByDescending { it.lastMessage?.sentAt ?: Long.MIN_VALUE })
    }

    /** [PAGE_SIZE] messages with [other] sent before [beforeMs] (the latest when null), oldest first. */
    suspend fun messages(me: UUID, other: UUID, beforeMs: Long?): MessagesResponse {
        requireFriends(me, other)
        val page = messages.page(me, other, beforeMs?.let(Instant::ofEpochMilli), PAGE_SIZE + 1)
        return MessagesResponse(items = page.take(PAGE_SIZE).reversed(), hasMore = page.size > PAGE_SIZE)
    }

    suspend fun send(me: UUID, other: UUID, body: String): MessageResponse {
        val text = body.trim()
        if (text.isEmpty()) throw BadRequestException("The message is empty")
        if (text.length > MAX_MESSAGE) throw BadRequestException("The message is too long")
        requireFriends(me, other)
        val message = messages.add(me, other, text)
        hub.send(other, SocialEvent.Typing(me.toString(), typing = false))
        hub.send(other, SocialEvent.Message(message))
        hub.send(me, SocialEvent.Message(message))
        pushCounts(other)
        return message
    }

    /** The user saw the chat with [other]: their messages are read, and [other] is told. */
    suspend fun markRead(me: UUID, other: UUID) {
        if (messages.markRead(me, other) == 0) return
        hub.send(other, SocialEvent.Read(me.toString()))
        pushCounts(me)
    }

    /** Tells [other] that the user is typing to them (friends only; others are ignored). */
    suspend fun typing(me: UUID, other: UUID, typing: Boolean) {
        if (friendships.find(me, other)?.accepted == true) hub.send(other, SocialEvent.Typing(me.toString(), typing))
    }

    suspend fun notifications(me: UUID): NotificationsResponse {
        val items = notifications.list(me, NOTIFICATIONS_LIMIT)
        return NotificationsResponse(withActorStatus(me, items), notifications.unreadCount(me))
    }

    suspend fun markNotificationsRead(me: UUID) {
        notifications.markAllRead(me)
        pushCounts(me)
    }

    suspend fun counts(me: UUID) = SocialCountsResponse(
        unreadNotifications = notifications.unreadCount(me),
        unreadMessages = messages.unreadBySender(me).values.sum(),
        pendingRequests = friendships.links(me).count { !it.accepted && it.addresseeId == me },
    )

    override suspend fun areFriends(a: UUID, b: UUID): Boolean = friendships.find(a, b)?.accepted == true

    override suspend fun memberAdded(playlistId: UUID, ownerId: UUID, memberId: UUID) =
        notify(memberId, NotificationType.PLAYLIST_INVITE, ownerId, playlistId)

    override suspend fun invitedToEvent(eventId: UUID, ownerId: UUID, userId: UUID) =
        notify(userId, NotificationType.EVENT_INVITE, ownerId, eventId = eventId)

    private suspend fun notify(userId: UUID, type: NotificationType, actorId: UUID, playlistId: UUID? = null, eventId: UUID? = null) {
        val id = notifications.add(userId, type, actorId, playlistId, eventId)
        notifications.get(id)?.let { hub.send(userId, SocialEvent.Notification(withActorStatus(userId, listOf(it)).single())) }
        pushCounts(userId)
    }

    private suspend fun friendsChanged(a: UUID, b: UUID) {
        hub.send(a, SocialEvent.FriendsChanged)
        hub.send(b, SocialEvent.FriendsChanged)
    }

    private suspend fun pushCounts(userId: UUID) = hub.send(userId, SocialEvent.Counts(counts(userId)))

    private suspend fun requireFriends(me: UUID, other: UUID) {
        if (!areFriends(me, other)) throw ForbiddenException("You can only chat with your friends", ApiErrors.NOT_FRIENDS)
    }

    private suspend fun status(me: UUID, other: UUID): FriendshipStatus {
        if (me == other) return FriendshipStatus.SELF
        return friendships.find(me, other).statusFor(me)
    }

    private suspend fun withStatus(me: UUID, users: List<PublicUserResponse>): List<PublicUserResponse> {
        val links = friendships.links(me).associateBy { it.other(me).toString() }
        return users.map { it.copy(friendship = links[it.id].statusFor(me)) }
    }

    private suspend fun withActorStatus(me: UUID, items: List<NotificationResponse>): List<NotificationResponse> {
        val links = friendships.links(me).associateBy { it.other(me).toString() }
        return items.map { item -> item.copy(actor = item.actor?.let { it.copy(friendship = links[it.id].statusFor(me)) }) }
    }

    private fun FriendshipsRepository.Link.other(me: UUID) = if (requesterId == me) addresseeId else requesterId

    private fun FriendshipsRepository.Link?.statusFor(me: UUID) = when {
        this == null -> FriendshipStatus.NONE
        accepted -> FriendshipStatus.FRIENDS
        requesterId == me -> FriendshipStatus.REQUEST_SENT
        else -> FriendshipStatus.REQUEST_RECEIVED
    }

    private companion object {
        const val MAX_QUERY = 64
        const val SEARCH_LIMIT = 20
        const val PAGE_SIZE = 40
        const val MAX_MESSAGE = 2000
        const val NOTIFICATIONS_LIMIT = 50
    }
}
