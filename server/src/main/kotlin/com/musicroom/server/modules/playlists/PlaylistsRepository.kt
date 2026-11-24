package com.musicroom.server.modules.playlists

import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.playlists.entities.Playlist
import com.musicroom.server.modules.playlists.entities.PlaylistCoversTable
import com.musicroom.server.modules.playlists.entities.PlaylistMembersTable
import com.musicroom.server.modules.playlists.entities.PlaylistsTable
import com.musicroom.shared.dto.PlaylistEditLicense
import com.musicroom.shared.dto.PlaylistVisibility
import java.time.OffsetDateTime
import java.util.UUID
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert

/** Playlists and their photos. Members and songs are in their own repositories. */
class PlaylistsRepository(private val db: DatabaseModule) {

    suspend fun create(
        ownerId: UUID,
        name: String,
        visibility: PlaylistVisibility,
        editLicense: PlaylistEditLicense,
    ): Playlist = db.query {
        val now = OffsetDateTime.now()
        val id = PlaylistsTable.insertAndGetId {
            it[PlaylistsTable.ownerId] = ownerId
            it[PlaylistsTable.name] = name
            it[PlaylistsTable.visibility] = visibility.key
            it[PlaylistsTable.editLicense] = editLicense.key
            it[revision] = 0
            it[createdAt] = now
            it[updatedAt] = now
        }
        Playlist(id.value, ownerId, name, visibility, editLicense, revision = 0, coverUpdatedAt = null, updatedAt = now)
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

    /** Public playlists, most recently changed first, optionally those whose name contains [query]. */
    suspend fun public(query: String, limit: Int): List<Playlist> = db.query {
        val words = query.trim().lowercase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        PlaylistsTable.selectAll()
            .where {
                val isPublic = PlaylistsTable.visibility eq PlaylistVisibility.PUBLIC.key
                if (words.isEmpty()) isPublic else isPublic and (PlaylistsTable.name.lowerCase() like "%$words%")
            }
            .orderBy(PlaylistsTable.updatedAt, SortOrder.DESC)
            .limit(limit)
            .map { it.toPlaylist() }
    }

    /** Null arguments are left unchanged. */
    suspend fun update(id: UUID, name: String?, visibility: PlaylistVisibility?, editLicense: PlaylistEditLicense?): Playlist? = db.query {
        PlaylistsTable.update({ PlaylistsTable.id eq id }) {
            if (name != null) it[PlaylistsTable.name] = name
            if (visibility != null) it[PlaylistsTable.visibility] = visibility.key
            if (editLicense != null) it[PlaylistsTable.editLicense] = editLicense.key
            it[updatedAt] = OffsetDateTime.now()
        }
        PlaylistsTable.selectAll().where { PlaylistsTable.id eq id }.singleOrNull()?.toPlaylist()
    }

    /** Marks the playlist as changed: it moves to the top of the lists and gets a new revision. */
    suspend fun touch(id: UUID) {
        db.query {
            PlaylistsTable.update({ PlaylistsTable.id eq id }) {
                it[updatedAt] = OffsetDateTime.now()
                it[revision] = revision + 1
            }
        }
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
        visibility = PlaylistVisibility.fromKey(this[PlaylistsTable.visibility]),
        editLicense = PlaylistEditLicense.fromKey(this[PlaylistsTable.editLicense]),
        revision = this[PlaylistsTable.revision],
        coverUpdatedAt = this[PlaylistsTable.coverUpdatedAt],
        updatedAt = this[PlaylistsTable.updatedAt],
    )
}
