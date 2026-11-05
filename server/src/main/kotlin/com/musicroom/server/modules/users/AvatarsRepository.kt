package com.musicroom.server.modules.users

import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.users.entities.UserAvatarsTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import java.time.OffsetDateTime
import java.util.UUID

class AvatarsRepository(private val db: DatabaseModule) {

    suspend fun save(userId: UUID, jpeg: ByteArray) {
        db.query {
            UserAvatarsTable.upsert {
                it[UserAvatarsTable.userId] = userId
                it[image] = jpeg
                it[updatedAt] = OffsetDateTime.now()
            }
        }
    }

    suspend fun find(userId: UUID): ByteArray? = db.query {
        UserAvatarsTable.selectAll().where { UserAvatarsTable.userId eq userId }.singleOrNull()?.get(UserAvatarsTable.image)
    }

    suspend fun delete(userId: UUID) {
        db.query { UserAvatarsTable.deleteWhere { UserAvatarsTable.userId eq userId } }
    }
}
