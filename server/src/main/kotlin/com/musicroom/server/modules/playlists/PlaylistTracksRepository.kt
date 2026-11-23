package com.musicroom.server.modules.playlists

import com.musicroom.server.common.exceptions.ConflictException
import com.musicroom.server.common.exceptions.NotFoundException
import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.playlists.entities.PlaylistTracksTable
import com.musicroom.server.modules.playlists.entities.PlaylistsTable
import com.musicroom.server.modules.users.entities.UsersTable
import com.musicroom.shared.ApiErrors
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
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

/**
 * Songs of the playlists, in order. Adding and moving first lock the playlist's row (SELECT ... FOR UPDATE), so
 * simultaneous edits of one playlist run one after the other and each works on the order the previous one left.
 */
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
        lockPlaylist(playlistId)
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

    /**
     * Moves [entryId] right after [afterEntryId] (to the top when null). A song removed meanwhile is a 404; a
     * neighbour removed meanwhile is a 409, as the place the user aimed at no longer exists.
     */
    suspend fun move(playlistId: UUID, entryId: UUID, afterEntryId: UUID?) {
        db.query {
            lockPlaylist(playlistId)
            val positions = PlaylistTracksTable.select(PlaylistTracksTable.id, PlaylistTracksTable.position)
                .where { PlaylistTracksTable.playlistId eq playlistId }
                .orderBy(PlaylistTracksTable.position to SortOrder.ASC, PlaylistTracksTable.addedAt to SortOrder.ASC)
                .map { it[PlaylistTracksTable.id].value to it[PlaylistTracksTable.position] }
            val order = positions.map { it.first }
            if (entryId !in order) throw NotFoundException("This song is not in the playlist")
            if (afterEntryId == entryId) return@query
            val others = order - entryId
            val index = if (afterEntryId == null) {
                0
            } else {
                val neighbour = others.indexOf(afterEntryId)
                if (neighbour < 0) throw ConflictException("The playlist changed, try again", ApiErrors.PLAYLIST_TRACK_MOVED)
                neighbour + 1
            }
            val stored = positions.toMap()
            (others.take(index) + entryId + others.drop(index)).forEachIndexed { position, id ->
                if (stored[id] != position) {
                    PlaylistTracksTable.update({ PlaylistTracksTable.id eq id }) { it[PlaylistTracksTable.position] = position }
                }
            }
        }
    }

    /** False if there is no such entry in this playlist. */
    suspend fun remove(playlistId: UUID, entryId: UUID): Boolean = db.query {
        PlaylistTracksTable.deleteWhere { (PlaylistTracksTable.playlistId eq playlistId) and (PlaylistTracksTable.id eq entryId) } == 1
    }

    private fun lockPlaylist(playlistId: UUID) {
        PlaylistsTable.select(PlaylistsTable.id).where { PlaylistsTable.id eq playlistId }.forUpdate().singleOrNull()
            ?: throw NotFoundException("Playlist not found")
    }
}
