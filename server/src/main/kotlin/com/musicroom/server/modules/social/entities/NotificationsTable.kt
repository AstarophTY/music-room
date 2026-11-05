package com.musicroom.server.modules.social.entities

import com.musicroom.server.modules.events.entities.EventsTable
import com.musicroom.server.modules.playlists.entities.PlaylistsTable
import com.musicroom.server.modules.users.entities.UsersTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object NotificationsTable : UUIDTable("notifications") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    /** NotificationType in lower case. */
    val type = varchar("type", 32)
    val actorId = reference("actor_id", UsersTable, onDelete = ReferenceOption.CASCADE).nullable()
    val playlistId = reference("playlist_id", PlaylistsTable, onDelete = ReferenceOption.CASCADE).nullable()
    val eventId = reference("event_id", EventsTable, onDelete = ReferenceOption.CASCADE).nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val readAt = timestampWithTimeZone("read_at").nullable()
}
