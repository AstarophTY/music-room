package com.musicroom.server.modules.playlists.entities

import com.musicroom.server.modules.users.entities.UsersTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object PlaylistsTable : UUIDTable("playlists") {
    val ownerId = reference("owner_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 64)
    val visibility = varchar("visibility", 16)
    val editLicense = varchar("edit_license", 16)
    val revision = long("revision")
    val coverUpdatedAt = timestampWithTimeZone("cover_updated_at").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}
