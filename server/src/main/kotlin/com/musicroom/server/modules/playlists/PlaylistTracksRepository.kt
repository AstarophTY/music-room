package com.musicroom.server.modules.playlists

import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.playlists.entities.PlaylistTracksTable
import com.musicroom.server.modules.users.entities.UsersTable
import com.musicroom.shared.MusicRoomJson
import com.musicroom.shared.dto.PlaylistTrackResponse
import com.musicroom.shared.dto.TrackResponse
import java.time.OffsetDateTime
import java.util.UUID
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll

/** Songs of the playlists, in order. */
class PlaylistTracksRepository(private val db: DatabaseModule) {

    suspend fun list(playlistId: UUID): List<PlaylistTrackResponse> = db.query {
        PlaylistTracksTable.join(UsersTable, JoinType.LEFT, PlaylistTracksTable.addedBy, UsersTable.id)
            .selectAll().where { PlaylistTracksTable.playlistId eq playlistId }
            .orderBy(PlaylistTracksTable.position to SortOrder.ASC, PlaylistTracksTable.addedAt to SortOrder.ASC)
            .map {
                PlaylistTrackResponse(
                    entryId = it[PlaylistTracksTable.id].value.toString(),
                    track = MusicRoomJson.decodeFromString(TrackResponse.serializer(), it[PlaylistTracksTable.track]),
                    addedBy = it.getOrNull(UsersTable.displayName).orEmpty(),
                    addedById = it.getOrNull(UsersTable.id)?.value?.toString(),
                )
            }
    }

    suspend fun count(playlistId: UUID): Int = db.query {
        PlaylistTracksTable.selectAll().where { PlaylistTracksTable.playlistId eq playlistId }.count().toInt()
    }

    /** Adds [track] at the end. False if it already is in the playlist. */
    suspend fun add(playlistId: UUID, track: TrackResponse, addedBy: UUID): Boolean = db.query {
        val last = PlaylistTracksTable.selectAll().where { PlaylistTracksTable.playlistId eq playlistId }
            .orderBy(PlaylistTracksTable.position, SortOrder.DESC).limit(1)
            .singleOrNull()?.get(PlaylistTracksTable.position) ?: -1
        PlaylistTracksTable.insertIgnore {
            it[PlaylistTracksTable.playlistId] = playlistId
            it[spotifyId] = track.id
            it[PlaylistTracksTable.track] = MusicRoomJson.encodeToString(TrackResponse.serializer(), track)
            it[position] = last + 1
            it[PlaylistTracksTable.addedBy] = addedBy
            it[addedAt] = OffsetDateTime.now()
        }.insertedCount == 1
    }

    /** False if there is no such entry in this playlist. */
    suspend fun remove(playlistId: UUID, entryId: UUID): Boolean = db.query {
        PlaylistTracksTable.deleteWhere { (PlaylistTracksTable.playlistId eq playlistId) and (PlaylistTracksTable.id eq entryId) } == 1
    }
}
