package com.musicroom.server.modules.users

import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.users.entities.User
import com.musicroom.server.modules.users.entities.UsersTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.OffsetDateTime
import java.util.UUID

class UsersRepository(private val db: DatabaseModule) {

    suspend fun findById(id: UUID): User? = db.query {
        UsersTable.selectAll().where { UsersTable.id eq id }.singleOrNull()?.toUser()
    }

    suspend fun findByEmail(email: String): User? = db.query {
        UsersTable.selectAll().where { UsersTable.email eq email }.singleOrNull()?.toUser()
    }

    suspend fun create(email: String, displayName: String, passwordHash: String?): User = db.query {
        val now = OffsetDateTime.now()
        val id = UsersTable.insertAndGetId {
            it[UsersTable.email] = email
            it[UsersTable.displayName] = displayName
            it[UsersTable.passwordHash] = passwordHash
            it[createdAt] = now
            it[updatedAt] = now
        }
        User(id.value, email, passwordHash, displayName, emailVerified = false, createdAt = now)
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
