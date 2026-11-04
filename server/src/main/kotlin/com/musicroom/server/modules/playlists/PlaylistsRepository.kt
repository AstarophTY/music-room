package com.musicroom.server.modules.playlists

import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.playlists.entities.Playlist
import com.musicroom.server.modules.playlists.entities.PlaylistCoversTable
import com.musicroom.server.modules.playlists.entities.PlaylistMembersTable
import com.musicroom.server.modules.playlists.entities.PlaylistsTable
import com.musicroom.shared.dto.PlaylistAccess
import com.musicroom.shared.dto.PlaylistType
import java.time.OffsetDateTime
import java.util.UUID
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert

/** Playlists and their photos. Members and songs are in their own repositories. */
class PlaylistsRepository(private val db: DatabaseModule) {

    suspend fun create(ownerId: UUID, name: String, type: PlaylistType): Playlist = db.query {
        val now = OffsetDateTime.now()
        val id = PlaylistsTable.insertAndGetId {
            it[PlaylistsTable.ownerId] = ownerId
            it[PlaylistsTable.name] = name
            it[PlaylistsTable.type] = type.key
            it[createdAt] = now
            it[updatedAt] = now
        }
        Playlist(id.value, ownerId, name, type, coverUpdatedAt = null, updatedAt = now)
    }

    suspend fun find(id: UUID): Playlist? = db.query {
        PlaylistsTable.selectAll().where { PlaylistsTable.id eq id }.singleOrNull()?.toPlaylist()
    }

    /** Owned or shared with [userId], most recently changed first. */
    suspend fun forUser(userId: UUID): List<Playlist> = db.query {
        val memberOf = PlaylistMembersTable.selectAll().where { PlaylistMembersTable.userId eq userId }
            .map { it[PlaylistMembersTable.playlistId].value }
        PlaylistsTable.selectAll()
            .where { (PlaylistsTable.ownerId eq userId) or (PlaylistsTable.id inList memberOf) }
            .orderBy(PlaylistsTable.updatedAt, SortOrder.DESC)
            .map { it.toPlaylist() }
    }

    /** Null arguments are left unchanged. Turning it personal makes every member read-only, in the same transaction. */
    suspend fun update(id: UUID, name: String?, type: PlaylistType?): Playlist? = db.query {
        PlaylistsTable.update({ PlaylistsTable.id eq id }) {
            if (name != null) it[PlaylistsTable.name] = name
            if (type != null) it[PlaylistsTable.type] = type.key
            it[updatedAt] = OffsetDateTime.now()
        }
        if (type == PlaylistType.PERSONAL) {
            PlaylistMembersTable.update({ PlaylistMembersTable.playlistId eq id }) { it[access] = PlaylistAccess.READ.key }
        }
        PlaylistsTable.selectAll().where { PlaylistsTable.id eq id }.singleOrNull()?.toPlaylist()
    }

    /** Marks the playlist as changed, so it moves to the top of the lists. */
    suspend fun touch(id: UUID) {
        db.query { PlaylistsTable.update({ PlaylistsTable.id eq id }) { it[updatedAt] = OffsetDateTime.now() } }
    }

    suspend fun delete(id: UUID) {
        db.query { PlaylistsTable.deleteWhere { PlaylistsTable.id eq id } }
    }

    suspend fun saveCover(id: UUID, jpeg: ByteArray) {
        db.query {
            val now = OffsetDateTime.now()
            PlaylistCoversTable.upsert {
                it[playlistId] = id
                it[image] = jpeg
                it[updatedAt] = now
            }
            PlaylistsTable.update({ PlaylistsTable.id eq id }) {
                it[coverUpdatedAt] = now
                it[updatedAt] = now
            }
        }
    }

    suspend fun deleteCover(id: UUID) {
        db.query {
            PlaylistCoversTable.deleteWhere { playlistId eq id }
            PlaylistsTable.update({ PlaylistsTable.id eq id }) {
                it[coverUpdatedAt] = null
                it[updatedAt] = OffsetDateTime.now()
            }
        }
    }

    suspend fun cover(id: UUID): ByteArray? = db.query {
        PlaylistCoversTable.selectAll().where { PlaylistCoversTable.playlistId eq id }.singleOrNull()?.get(PlaylistCoversTable.image)
    }

    private fun ResultRow.toPlaylist() = Playlist(
        id = this[PlaylistsTable.id].value,
        ownerId = this[PlaylistsTable.ownerId].value,
        name = this[PlaylistsTable.name],
        type = PlaylistType.fromKey(this[PlaylistsTable.type]),
        coverUpdatedAt = this[PlaylistsTable.coverUpdatedAt],
        updatedAt = this[PlaylistsTable.updatedAt],
    )
}
