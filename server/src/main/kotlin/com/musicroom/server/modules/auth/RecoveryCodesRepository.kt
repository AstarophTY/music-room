package com.musicroom.server.modules.auth

import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.auth.entities.RecoveryCodesTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.update
import java.time.OffsetDateTime
import java.util.UUID

class RecoveryCodesRepository(private val db: DatabaseModule) {

    /** Removes the old codes and stores the new ones (hashed). */
    suspend fun replaceAll(userId: UUID, codeHashes: List<String>) {
        db.query {
            RecoveryCodesTable.deleteWhere { RecoveryCodesTable.userId eq userId }
            val now = OffsetDateTime.now()
            RecoveryCodesTable.batchInsert(codeHashes) { hash ->
                this[RecoveryCodesTable.userId] = userId
                this[RecoveryCodesTable.codeHash] = hash
                this[RecoveryCodesTable.createdAt] = now
            }
        }
    }

    /** Atomic: true once per code, false if it does not exist or was already used. */
    suspend fun consume(userId: UUID, codeHash: String): Boolean = db.query {
        RecoveryCodesTable.update({
            (RecoveryCodesTable.userId eq userId) and (RecoveryCodesTable.codeHash eq codeHash) and
                RecoveryCodesTable.usedAt.isNull()
        }) { it[usedAt] = OffsetDateTime.now() } == 1
    }

    suspend fun deleteAll(userId: UUID) {
        db.query { RecoveryCodesTable.deleteWhere { RecoveryCodesTable.userId eq userId } }
    }
}
