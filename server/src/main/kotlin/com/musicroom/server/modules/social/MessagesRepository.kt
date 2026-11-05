package com.musicroom.server.modules.social

import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.social.entities.MessagesTable
import com.musicroom.shared.dto.MessageResponse
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.select

/** Chat messages between two users. */
class MessagesRepository(private val db: DatabaseModule) {

    suspend fun add(sender: UUID, recipient: UUID, body: String): MessageResponse = db.query {
        val now = OffsetDateTime.now()
        val id = MessagesTable.insertAndGetId {
            it[senderId] = sender
            it[recipientId] = recipient
            it[MessagesTable.body] = body
            it[createdAt] = now
        }.value
        MessageResponse(id.toString(), sender.toString(), recipient.toString(), body, now.toInstant().toEpochMilli())
    }

    /** Up to [limit] messages between [a] and [b] sent before [before] (all when null), newest first. */
    suspend fun page(a: UUID, b: UUID, before: Instant?, limit: Int): List<MessageResponse> = db.query {
        val older = before?.let { MessagesTable.createdAt less OffsetDateTime.ofInstant(it, ZoneOffset.UTC) } ?: Op.TRUE
        MessagesTable.selectAll().where { pair(a, b) and older }
            .orderBy(MessagesTable.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { it.toMessage() }
    }

    suspend fun last(a: UUID, b: UUID): MessageResponse? = page(a, b, before = null, limit = 1).firstOrNull()

    /** Marks what [other] sent to [reader] as read. Returns how many were unread. */
    suspend fun markRead(reader: UUID, other: UUID): Int = db.query {
        MessagesTable.update({
            (MessagesTable.recipientId eq reader) and (MessagesTable.senderId eq other) and MessagesTable.readAt.isNull()
        }) { it[readAt] = OffsetDateTime.now() }
    }

    /** Unread messages of [userId], by sender. */
    suspend fun unreadBySender(userId: UUID): Map<UUID, Int> = db.query {
        val count = MessagesTable.id.count()
        MessagesTable.select(MessagesTable.senderId, count)
            .where { (MessagesTable.recipientId eq userId) and MessagesTable.readAt.isNull() }
            .groupBy(MessagesTable.senderId)
            .associate { it[MessagesTable.senderId].value to it[count].toInt() }
    }

    private fun pair(a: UUID, b: UUID): Op<Boolean> =
        ((MessagesTable.senderId eq a) and (MessagesTable.recipientId eq b)) or
            ((MessagesTable.senderId eq b) and (MessagesTable.recipientId eq a))

    private fun ResultRow.toMessage() = MessageResponse(
        id = this[MessagesTable.id].value.toString(),
        senderId = this[MessagesTable.senderId].value.toString(),
        recipientId = this[MessagesTable.recipientId].value.toString(),
        body = this[MessagesTable.body],
        sentAt = this[MessagesTable.createdAt].toInstant().toEpochMilli(),
        read = this[MessagesTable.readAt] != null,
    )
}
