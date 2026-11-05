package com.musicroom.server.modules.events.entities

import com.musicroom.server.modules.users.entities.UsersTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object EventsTable : UUIDTable("events") {
    val ownerId = reference("owner_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 64)
    val description = varchar("description", 280)
    /** EventVisibility in lower case. */
    val visibility = varchar("visibility", 16)
    /** EventVoteLicense in lower case. */
    val voteLicense = varchar("vote_license", 16)
    val latitude = double("latitude").nullable()
    val longitude = double("longitude").nullable()
    val radiusMeters = integer("radius_m").nullable()
    val startsAt = timestampWithTimeZone("starts_at").nullable()
    val endsAt = timestampWithTimeZone("ends_at").nullable()
    /** EventStatus in lower case. */
    val status = varchar("status", 16)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}
