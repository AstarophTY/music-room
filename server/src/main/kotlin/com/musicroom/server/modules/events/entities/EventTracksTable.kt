package com.musicroom.server.modules.events.entities

import com.musicroom.server.modules.users.entities.UsersTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object EventTracksTable : UUIDTable("event_tracks") {
    val eventId = reference("event_id", EventsTable, onDelete = ReferenceOption.CASCADE)
    val spotifyId = varchar("spotify_id", 64)
    /** TrackResponse as JSON. */
    val track = text("track")
    val proposedBy = reference("proposed_by", UsersTable, onDelete = ReferenceOption.SET_NULL).nullable()
    /** Number of event_votes rows of this song, changed in the same transaction. */
    val votes = integer("votes")
    /** "queued", "next", "playing" or "played". */
    val status = varchar("status", 16)
    val createdAt = timestampWithTimeZone("created_at")
    val playedAt = timestampWithTimeZone("played_at").nullable()

    const val QUEUED = "queued"
    const val NEXT = "next"
    const val PLAYING = "playing"
    const val PLAYED = "played"
}
