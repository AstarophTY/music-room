package com.musicroom.server.modules.social.entities

import com.musicroom.server.modules.users.entities.UsersTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object FriendshipsTable : Table("friendships") {
    val requesterId = reference("requester_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val addresseeId = reference("addressee_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    /** "pending" or "accepted". */
    val status = varchar("status", 16)
    val createdAt = timestampWithTimeZone("created_at")
    val acceptedAt = timestampWithTimeZone("accepted_at").nullable()

    override val primaryKey = PrimaryKey(requesterId, addresseeId)

    const val PENDING = "pending"
    const val ACCEPTED = "accepted"
}
