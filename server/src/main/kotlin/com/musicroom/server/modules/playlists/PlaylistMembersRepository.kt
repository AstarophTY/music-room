package com.musicroom.server.modules.playlists

import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.playlists.entities.PlaylistMembersTable
import com.musicroom.server.modules.users.entities.UsersTable
import com.musicroom.shared.dto.AvatarColor
import com.musicroom.shared.dto.PlaylistAccess
import com.musicroom.shared.dto.PlaylistMemberResponse
import java.time.OffsetDateTime
import java.util.UUID
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

/** Users added to a playlist besides its owner, with their rights. */
class PlaylistMembersRepository(private val db: DatabaseModule) {

    /** Null when [userId] is not a member (the owner is not one either). */
    suspend fun access(playlistId: UUID, userId: UUID): PlaylistAccess? = db.query {
        PlaylistMembersTable.selectAll()
            .where { (PlaylistMembersTable.playlistId eq playlistId) and (PlaylistMembersTable.userId eq userId) }
            .singleOrNull()?.let { PlaylistAccess.fromKey(it[PlaylistMembersTable.access]) }
    }

    /** In the order they were added. */
    suspend fun list(playlistId: UUID): List<PlaylistMemberResponse> = db.query {
        PlaylistMembersTable.join(UsersTable, JoinType.INNER, PlaylistMembersTable.userId, UsersTable.id)
            .selectAll().where { PlaylistMembersTable.playlistId eq playlistId }
            .orderBy(PlaylistMembersTable.addedAt, SortOrder.ASC)
            .map { it.toMember(PlaylistAccess.fromKey(it[PlaylistMembersTable.access])) }
    }

    suspend fun count(playlistId: UUID): Int = db.query {
        PlaylistMembersTable.selectAll().where { PlaylistMembersTable.playlistId eq playlistId }.count().toInt()
    }

    /** False if the user already is a member. */
    suspend fun add(playlistId: UUID, userId: UUID, access: PlaylistAccess): Boolean = db.query {
        PlaylistMembersTable.insertIgnore {
            it[PlaylistMembersTable.playlistId] = playlistId
            it[PlaylistMembersTable.userId] = userId
            it[PlaylistMembersTable.access] = access.key
            it[addedAt] = OffsetDateTime.now()
        }.insertedCount == 1
    }

    /** False if the user is not a member. */
    suspend fun update(playlistId: UUID, userId: UUID, access: PlaylistAccess): Boolean = db.query {
        PlaylistMembersTable.update({ (PlaylistMembersTable.playlistId eq playlistId) and (PlaylistMembersTable.userId eq userId) }) {
            it[PlaylistMembersTable.access] = access.key
        } == 1
    }

    /** False if the user is not a member. */
    suspend fun remove(playlistId: UUID, userId: UUID): Boolean = db.query {
        PlaylistMembersTable.deleteWhere {
            (PlaylistMembersTable.playlistId eq playlistId) and (PlaylistMembersTable.userId eq userId)
        } == 1
    }

    /** [userId] shown as a member with [access] (used for the owner). */
    suspend fun user(userId: UUID, access: PlaylistAccess): PlaylistMemberResponse? = db.query {
        UsersTable.selectAll().where { UsersTable.id eq userId }.singleOrNull()?.toMember(access)
    }

    private fun ResultRow.toMember(access: PlaylistAccess) = PlaylistMemberResponse(
        userId = this[UsersTable.id].value.toString(),
        displayName = this[UsersTable.displayName],
        access = access,
        avatarColor = AvatarColor.fromKey(this[UsersTable.avatarColor]),
        avatarVersion = this[UsersTable.avatarUpdatedAt]?.toInstant()?.toEpochMilli(),
    )
}
