package com.musicroom.server.modules.social.entities

import com.musicroom.server.modules.users.entities.UsersTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object MessagesTable : UUIDTable("messages") {
    val senderId = reference("sender_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val recipientId = reference("recipient_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val body = varchar("body", 2000)
    val createdAt = timestampWithTimeZone("created_at")
    val readAt = timestampWithTimeZone("read_at").nullable()
}
