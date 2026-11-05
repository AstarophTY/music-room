package com.musicroom.server.modules.playlists.entities

import com.musicroom.server.modules.users.entities.UsersTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object PlaylistMembersTable : Table("playlist_members") {
    val playlistId = reference("playlist_id", PlaylistsTable, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val access = varchar("access", 16)
    val addedAt = timestampWithTimeZone("added_at")

    override val primaryKey = PrimaryKey(playlistId, userId)
}
