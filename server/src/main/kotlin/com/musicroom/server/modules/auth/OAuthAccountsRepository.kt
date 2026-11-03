package com.musicroom.server.modules.auth

import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.auth.entities.OAuthAccountsTable
import com.musicroom.shared.dto.OAuthProvider
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.OffsetDateTime
import java.util.UUID

class OAuthAccountsRepository(private val db: DatabaseModule) {

    suspend fun findUserId(provider: OAuthProvider, providerUserId: String): UUID? = db.query {
        OAuthAccountsTable.selectAll()
            .where {
                (OAuthAccountsTable.provider eq provider.key) and (OAuthAccountsTable.providerUserId eq providerUserId)
            }
            .singleOrNull()
            ?.get(OAuthAccountsTable.userId)?.value
    }

    suspend fun link(userId: UUID, provider: OAuthProvider, providerUserId: String, email: String?) {
        db.query {
            OAuthAccountsTable.insert {
                it[OAuthAccountsTable.userId] = userId
                it[OAuthAccountsTable.provider] = provider.key
                it[OAuthAccountsTable.providerUserId] = providerUserId
                it[OAuthAccountsTable.email] = email
                it[createdAt] = OffsetDateTime.now()
            }
        }
    }

    /** Value stored in the `provider` column. */
    private val OAuthProvider.key: String get() = name.lowercase()
}
