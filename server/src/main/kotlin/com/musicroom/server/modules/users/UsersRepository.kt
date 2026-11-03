package com.musicroom.server.modules.users

import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.users.entities.User
import com.musicroom.server.modules.users.entities.UsersTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.OffsetDateTime
import java.util.UUID

class UsersRepository(private val db: DatabaseModule) {

    suspend fun findById(id: UUID): User? = db.query {
        UsersTable.selectAll().where { UsersTable.id eq id }.singleOrNull()?.toUser()
    }

    suspend fun findByEmail(email: String): User? = db.query {
        UsersTable.selectAll().where { UsersTable.email eq email }.singleOrNull()?.toUser()
    }

    suspend fun create(
        email: String,
        displayName: String,
        passwordHash: String?,
        emailVerified: Boolean = false,
        termsAcceptedAt: OffsetDateTime? = null,
    ): User = db.query {
        val now = OffsetDateTime.now()
        val id = UsersTable.insertAndGetId {
            it[UsersTable.email] = email
            it[UsersTable.displayName] = displayName
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.emailVerified] = emailVerified
            it[UsersTable.termsAcceptedAt] = termsAcceptedAt
            it[createdAt] = now
            it[updatedAt] = now
        }
        User(id.value, email, passwordHash, displayName, emailVerified, createdAt = now)
    }

    suspend fun markEmailVerified(id: UUID) {
        db.query {
            UsersTable.update({ UsersTable.id eq id }) {
                it[emailVerified] = true
                it[updatedAt] = OffsetDateTime.now()
            }
        }
    }

    /**
     * An OAuth provider proved who owns this email, but the account was created with a password by someone
     * who never confirmed the email. That password may belong to an attacker: remove it and verify the email.
     */
    suspend fun takeOverUnverified(id: UUID) {
        db.query {
            UsersTable.update({ UsersTable.id eq id }) {
                it[emailVerified] = true
                it[passwordHash] = null
                it[updatedAt] = OffsetDateTime.now()
            }
        }
    }

    suspend fun updateDisplayName(id: UUID, displayName: String): User? = db.query {
        UsersTable.update({ UsersTable.id eq id }) {
            it[UsersTable.displayName] = displayName
            it[updatedAt] = OffsetDateTime.now()
        }
        UsersTable.selectAll().where { UsersTable.id eq id }.singleOrNull()?.toUser()
    }

    suspend fun updatePasswordHash(id: UUID, passwordHash: String) {
        db.query {
            UsersTable.update({ UsersTable.id eq id }) {
                it[UsersTable.passwordHash] = passwordHash
                it[updatedAt] = OffsetDateTime.now()
            }
        }
    }

    private fun ResultRow.toUser() = User(
        id = this[UsersTable.id].value,
        email = this[UsersTable.email],
        passwordHash = this[UsersTable.passwordHash],
        displayName = this[UsersTable.displayName],
        emailVerified = this[UsersTable.emailVerified],
        createdAt = this[UsersTable.createdAt],
    )
}
