package com.musicroom.server.modules.events.entities

import com.musicroom.server.modules.users.entities.UsersTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object EventInvitesTable : Table("event_invites") {
    val eventId = reference("event_id", EventsTable, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val invitedBy = reference("invited_by", UsersTable, onDelete = ReferenceOption.SET_NULL).nullable()
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(eventId, userId)
}
