package com.musicroom.server.modules.social

import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.events.entities.EventsTable
import com.musicroom.server.modules.playlists.entities.PlaylistsTable
import com.musicroom.server.modules.social.entities.NotificationsTable
import com.musicroom.server.modules.users.entities.UsersTable
import com.musicroom.shared.dto.NotificationResponse
import com.musicroom.shared.dto.NotificationType
import java.time.OffsetDateTime
import java.util.UUID
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

/** Notifications of each user, with who caused them and the playlist they are about. */
class NotificationsRepository(private val db: DatabaseModule) {

    suspend fun add(userId: UUID, type: NotificationType, actorId: UUID?, playlistId: UUID? = null, eventId: UUID? = null): UUID = db.query {
        NotificationsTable.insertAndGetId {
            it[NotificationsTable.userId] = userId
            it[NotificationsTable.type] = type.name.lowercase()
            it[NotificationsTable.actorId] = actorId
            it[NotificationsTable.playlistId] = playlistId
            it[NotificationsTable.eventId] = eventId
            it[createdAt] = OffsetDateTime.now()
        }.value
    }

    /** The latest [limit] notifications of [userId], newest first (friendship of the actor left to fill). */
    suspend fun list(userId: UUID, limit: Int): List<NotificationResponse> = query(NotificationsTable.userId eq userId, limit)

    suspend fun get(id: UUID): NotificationResponse? = query(NotificationsTable.id eq id, 1).firstOrNull()

    suspend fun unreadCount(userId: UUID): Int = db.query {
        NotificationsTable.selectAll().where { (NotificationsTable.userId eq userId) and NotificationsTable.readAt.isNull() }.count().toInt()
    }

    suspend fun markAllRead(userId: UUID) = db.query {
        NotificationsTable.update({ (NotificationsTable.userId eq userId) and NotificationsTable.readAt.isNull() }) {
            it[readAt] = OffsetDateTime.now()
        }
    }

    /** Marks the request [requesterId] sent to [userId] as read: it was just answered. */
    suspend fun markRequestAnswered(userId: UUID, requesterId: UUID) = db.query {
        NotificationsTable.update({
            (NotificationsTable.userId eq userId) and (NotificationsTable.actorId eq requesterId) and
                (NotificationsTable.type eq NotificationType.FRIEND_REQUEST.name.lowercase()) and NotificationsTable.readAt.isNull()
        }) { it[readAt] = OffsetDateTime.now() }
    }

    /** Removes the notifications of [type] that [actorId] caused to [userId] (a request taken back). */
    suspend fun delete(userId: UUID, type: NotificationType, actorId: UUID) = db.query {
        NotificationsTable.deleteWhere {
            (NotificationsTable.userId eq userId) and (NotificationsTable.type eq type.name.lowercase()) and
                (NotificationsTable.actorId eq actorId)
        }
    }

    private suspend fun query(where: Op<Boolean>, limit: Int): List<NotificationResponse> = db.query {
        NotificationsTable
            .join(UsersTable, JoinType.LEFT, NotificationsTable.actorId, UsersTable.id)
            .join(PlaylistsTable, JoinType.LEFT, NotificationsTable.playlistId, PlaylistsTable.id)
            .join(EventsTable, JoinType.LEFT, NotificationsTable.eventId, EventsTable.id)
            .selectAll().where { where }
            .orderBy(NotificationsTable.createdAt to SortOrder.DESC)
            .limit(limit)
            .mapNotNull { it.toNotification() }
    }

    /** Null for a type this version does not know. */
    private fun ResultRow.toNotification(): NotificationResponse? {
        val type = NotificationType.entries.firstOrNull { it.name.lowercase() == this[NotificationsTable.type] } ?: return null
        return NotificationResponse(
            id = this[NotificationsTable.id].value.toString(),
            type = type,
            actor = this.getOrNull(UsersTable.id)?.let { PeopleRepository.publicUser(this) },
            playlistId = this[NotificationsTable.playlistId]?.value?.toString(),
            playlistName = this.getOrNull(PlaylistsTable.name),
            eventId = this[NotificationsTable.eventId]?.value?.toString(),
            eventName = this.getOrNull(EventsTable.name),
            createdAt = this[NotificationsTable.createdAt].toInstant().toEpochMilli(),
            read = this[NotificationsTable.readAt] != null,
        )
    }
}
