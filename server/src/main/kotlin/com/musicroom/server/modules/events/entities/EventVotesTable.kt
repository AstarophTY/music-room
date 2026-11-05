package com.musicroom.server.modules.events.entities

import com.musicroom.server.modules.users.entities.UsersTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object EventVotesTable : Table("event_votes") {
    val eventId = reference("event_id", EventsTable, onDelete = ReferenceOption.CASCADE)
    val eventTrackId = reference("event_track_id", EventTracksTable, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val createdAt = timestampWithTimeZone("created_at")

    /** One vote per user and song: a second one is refused by the database, even when sent at the same time. */
    override val primaryKey = PrimaryKey(eventTrackId, userId)
}
