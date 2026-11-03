package com.musicroom.server.modules.auth

import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.auth.entities.RefreshToken
import com.musicroom.server.modules.auth.entities.RefreshTokensTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.OffsetDateTime
import java.util.UUID

class RefreshTokensRepository(private val db: DatabaseModule) {

    suspend fun create(userId: UUID, tokenHash: String, expiresAt: OffsetDateTime) {
        db.query {
            RefreshTokensTable.insert {
                it[RefreshTokensTable.userId] = userId
                it[RefreshTokensTable.tokenHash] = tokenHash
                it[RefreshTokensTable.expiresAt] = expiresAt
                it[createdAt] = OffsetDateTime.now()
            }
        }
    }

    suspend fun findByHash(tokenHash: String): RefreshToken? = db.query {
        RefreshTokensTable.selectAll().where { RefreshTokensTable.tokenHash eq tokenHash }.singleOrNull()?.let {
            RefreshToken(
                id = it[RefreshTokensTable.id].value,
                userId = it[RefreshTokensTable.userId].value,
                expiresAt = it[RefreshTokensTable.expiresAt],
                revokedAt = it[RefreshTokensTable.revokedAt],
            )
        }
    }

    /** Atomic: returns false if the token was already revoked (e.g. two refreshes at the same time). */
    suspend fun revoke(id: UUID): Boolean = db.query {
        RefreshTokensTable.update({ (RefreshTokensTable.id eq id) and RefreshTokensTable.revokedAt.isNull() }) {
            it[revokedAt] = OffsetDateTime.now()
        } == 1
    }

    suspend fun revokeByHash(tokenHash: String) {
        db.query {
            RefreshTokensTable.update({
                (RefreshTokensTable.tokenHash eq tokenHash) and RefreshTokensTable.revokedAt.isNull()
            }) { it[revokedAt] = OffsetDateTime.now() }
        }
    }

    suspend fun revokeAllForUser(userId: UUID) {
        db.query {
            RefreshTokensTable.update({
                (RefreshTokensTable.userId eq userId) and RefreshTokensTable.revokedAt.isNull()
            }) { it[revokedAt] = OffsetDateTime.now() }
        }
    }
}
