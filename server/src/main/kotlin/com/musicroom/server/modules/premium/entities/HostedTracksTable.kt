package com.musicroom.server.modules.premium.entities

import com.musicroom.server.modules.users.entities.UsersTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object HostedTracksTable : UUIDTable("hosted_tracks") {
    val ownerId = reference("owner_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val title = varchar("title", 100)
    val artist = varchar("artist", 100)
    val durationMs = long("duration_ms")
    val sizeBytes = integer("size_bytes")
    val createdAt = timestampWithTimeZone("created_at")
}
