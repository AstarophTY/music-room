package com.musicroom.server.modules.social

import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.users.entities.UsersTable
import com.musicroom.shared.dto.AvatarColor
import com.musicroom.shared.dto.PublicUserResponse
import java.util.UUID
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.selectAll

/** Users as others see them: name and photo, to search and to show friends. */
class PeopleRepository(private val db: DatabaseModule) {

    /** Users whose name contains [query] (any case), or whose email is exactly [query]; [excluding] is left out. */
    suspend fun search(query: String, excluding: UUID, limit: Int): List<PublicUserResponse> = db.query {
        // % and _ would be wildcards in LIKE.
        val words = query.lowercase().filter { it != '%' && it != '_' }
        UsersTable.selectAll()
            .where { ((UsersTable.displayName.lowerCase() like "%$words%") or (UsersTable.email eq query.lowercase())) and (UsersTable.id neq excluding) }
            .orderBy(UsersTable.displayName to SortOrder.ASC)
            .limit(limit)
            .map { publicUser(it) }
    }

    /** By id, in no particular order; unknown ids are left out. */
    suspend fun find(ids: Collection<UUID>): Map<UUID, PublicUserResponse> = if (ids.isEmpty()) {
        emptyMap()
    } else {
        db.query {
            UsersTable.selectAll().where { UsersTable.id inList ids }.associate { it[UsersTable.id].value to publicUser(it) }
        }
    }

    companion object {
        /** A row with the UsersTable columns as a public user (friendship to fill by the caller). */
        fun publicUser(row: ResultRow) = PublicUserResponse(
            id = row[UsersTable.id].value.toString(),
            displayName = row[UsersTable.displayName],
            avatarColor = AvatarColor.fromKey(row[UsersTable.avatarColor]),
            avatarVersion = row[UsersTable.avatarUpdatedAt]?.toInstant()?.toEpochMilli(),
        )
    }
}
