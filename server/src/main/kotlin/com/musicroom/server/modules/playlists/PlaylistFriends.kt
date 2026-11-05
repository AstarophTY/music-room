package com.musicroom.server.modules.playlists

import java.util.UUID

/** What the playlists need from the friends system: who can be invited by id, and telling a user they were added. */
interface PlaylistFriends {
    suspend fun areFriends(a: UUID, b: UUID): Boolean

    suspend fun memberAdded(playlistId: UUID, ownerId: UUID, memberId: UUID)
}
