package com.musicroom.server.modules.playlists.entities

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object PlaylistCoversTable : Table("playlist_covers") {
    val playlistId = reference("playlist_id", PlaylistsTable, onDelete = ReferenceOption.CASCADE)
    val image = binary("image")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(playlistId)
}
