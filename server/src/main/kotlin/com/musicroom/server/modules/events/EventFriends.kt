package com.musicroom.server.modules.events

import java.util.UUID

/** What the events need from the friends system: who can be invited, and telling a user they were invited. */
interface EventFriends {
    suspend fun areFriends(a: UUID, b: UUID): Boolean

    suspend fun invitedToEvent(eventId: UUID, ownerId: UUID, userId: UUID)
}
