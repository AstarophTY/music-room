package com.musicroom.server.modules.users

import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.users.entities.UserDetailsTable
import com.musicroom.shared.MusicRoomJson
import com.musicroom.shared.dto.UserDetailsResponse
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert

/** What users tell about themselves. Callers decide which part each audience may read. */
class UserDetailsRepository(private val db: DatabaseModule) {
    private val preferences = ListSerializer(String.serializer())

    suspend fun find(userId: UUID): UserDetailsResponse = db.query {
        UserDetailsTable.selectAll().where { UserDetailsTable.userId eq userId }.singleOrNull()?.let {
            UserDetailsResponse(
                publicInfo = it[UserDetailsTable.publicInfo],
                friendsInfo = it[UserDetailsTable.friendsInfo],
                privateInfo = it[UserDetailsTable.privateInfo],
                musicPreferences = MusicRoomJson.decodeFromString(preferences, it[UserDetailsTable.musicPreferences]),
            )
        } ?: UserDetailsResponse()
    }

    suspend fun save(userId: UUID, details: UserDetailsResponse) {
        db.query {
            UserDetailsTable.upsert {
                it[UserDetailsTable.userId] = userId
                it[publicInfo] = details.publicInfo
                it[friendsInfo] = details.friendsInfo
                it[privateInfo] = details.privateInfo
                it[musicPreferences] = MusicRoomJson.encodeToString(preferences, details.musicPreferences)
                it[updatedAt] = OffsetDateTime.now()
            }
        }
    }
}
