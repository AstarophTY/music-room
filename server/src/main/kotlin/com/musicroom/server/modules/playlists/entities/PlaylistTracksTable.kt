package com.musicroom.server.modules.playlists.entities

import com.musicroom.server.modules.users.entities.UsersTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object PlaylistTracksTable : UUIDTable("playlist_tracks") {
    val playlistId = reference("playlist_id", PlaylistsTable, onDelete = ReferenceOption.CASCADE)
    val spotifyId = varchar("spotify_id", 64)
    /** TrackResponse as JSON. */
    val track = text("track")
    val position = integer("position")
    val addedBy = reference("added_by", UsersTable, onDelete = ReferenceOption.SET_NULL).nullable()
    val addedAt = timestampWithTimeZone("added_at")
}
