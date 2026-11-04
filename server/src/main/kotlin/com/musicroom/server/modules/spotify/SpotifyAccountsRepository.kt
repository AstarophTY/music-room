package com.musicroom.server.modules.spotify

import com.musicroom.server.common.security.SecretBox
import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.spotify.entities.SpotifyAccount
import com.musicroom.server.modules.spotify.entities.SpotifyAccountsTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import java.time.OffsetDateTime
import java.util.UUID

class SpotifyAccountsRepository(private val db: DatabaseModule, private val secretBox: SecretBox) {

    suspend fun save(account: SpotifyAccount, scope: String?) {
        db.query {
            val now = OffsetDateTime.now()
            SpotifyAccountsTable.upsert {
                it[userId] = account.userId
                it[spotifyUserId] = account.spotifyUserId
                it[displayName] = account.displayName
                it[product] = account.product
                it[accessToken] = secretBox.encrypt(account.accessToken)
                it[refreshToken] = secretBox.encrypt(account.refreshToken)
                it[expiresAt] = account.expiresAt
                it[SpotifyAccountsTable.scope] = scope
                it[createdAt] = now
                it[updatedAt] = now
            }
        }
    }

    suspend fun find(userId: UUID): SpotifyAccount? = db.query {
        SpotifyAccountsTable.selectAll().where { SpotifyAccountsTable.userId eq userId }.singleOrNull()?.let {
            SpotifyAccount(
                userId = userId,
                spotifyUserId = it[SpotifyAccountsTable.spotifyUserId],
                displayName = it[SpotifyAccountsTable.displayName],
                product = it[SpotifyAccountsTable.product],
                accessToken = secretBox.decrypt(it[SpotifyAccountsTable.accessToken]),
                refreshToken = secretBox.decrypt(it[SpotifyAccountsTable.refreshToken]),
                expiresAt = it[SpotifyAccountsTable.expiresAt],
            )
        }
    }

    suspend fun updateTokens(userId: UUID, accessToken: String, refreshToken: String, expiresAt: OffsetDateTime) {
        db.query {
            SpotifyAccountsTable.update({ SpotifyAccountsTable.userId eq userId }) {
                it[SpotifyAccountsTable.accessToken] = secretBox.encrypt(accessToken)
                it[SpotifyAccountsTable.refreshToken] = secretBox.encrypt(refreshToken)
                it[SpotifyAccountsTable.expiresAt] = expiresAt
                it[updatedAt] = OffsetDateTime.now()
            }
        }
    }

    suspend fun delete(userId: UUID) {
        db.query { SpotifyAccountsTable.deleteWhere { SpotifyAccountsTable.userId eq userId } }
    }
}
