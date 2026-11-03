package com.musicroom.server.modules.users

import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.users.entities.User
import com.musicroom.server.modules.users.entities.UsersTable
import com.musicroom.shared.dto.AvatarColor
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.plus
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

    /** Null arguments are left unchanged. */
    suspend fun updateProfile(id: UUID, displayName: String?, avatarColor: AvatarColor?): User? = update(id) {
        if (displayName != null) it[UsersTable.displayName] = displayName
        if (avatarColor != null) it[UsersTable.avatarColor] = avatarColor.key
    }

    suspend fun updatePasswordHash(id: UUID, passwordHash: String) {
        update(id) { it[UsersTable.passwordHash] = passwordHash }
    }

    suspend fun markEmailVerified(id: UUID) {
        update(id) { it[UsersTable.emailVerified] = true }
    }

    /**
     * An OAuth provider proved who owns this email, but the account was created with a password by someone
     * who never confirmed the email. That password may belong to an attacker: remove it and verify the email.
     */
    suspend fun takeOverUnverified(id: UUID) {
        update(id) {
            it[UsersTable.emailVerified] = true
            it[UsersTable.passwordHash] = null
        }
    }

    suspend fun setAvatarUpdatedAt(id: UUID, updatedAt: OffsetDateTime?): User? =
        update(id) { it[UsersTable.avatarUpdatedAt] = updatedAt }

    suspend fun setPendingTotpSecret(id: UUID, encryptedSecret: String) {
        update(id) { it[UsersTable.totpPendingSecret] = encryptedSecret }
    }

    /** Turns 2FA on with the pending secret. [usedStep] is the step of the code that confirmed it. */
    suspend fun enableTotp(id: UUID, encryptedSecret: String, usedStep: Long): User? = update(id) {
        it[UsersTable.totpSecret] = encryptedSecret
        it[UsersTable.totpPendingSecret] = null
        it[UsersTable.totpLastStep] = usedStep
        it[UsersTable.twoFactorFailures] = 0
    }

    suspend fun disableTotp(id: UUID): User? = update(id) {
        it[UsersTable.totpSecret] = null
        it[UsersTable.totpPendingSecret] = null
        it[UsersTable.totpLastStep] = null
    }

    /** Atomic: false if this step (or a later one) was already used, so a code works only once. */
    suspend fun recordTotpStep(id: UUID, step: Long): Boolean = db.query {
        UsersTable.update({
            (UsersTable.id eq id) and (UsersTable.totpLastStep.isNull() or (UsersTable.totpLastStep less step))
        }) { it[totpLastStep] = step } == 1
    }

    /** Counts a wrong 2FA code; after [maxFailures] in a row, blocks 2FA codes for [lockMinutes]. */
    suspend fun registerTwoFactorFailure(id: UUID, maxFailures: Int, lockMinutes: Long) {
        db.query {
            UsersTable.update({ UsersTable.id eq id }) { it[twoFactorFailures] = twoFactorFailures + 1 }
            val failures = UsersTable.selectAll().where { UsersTable.id eq id }.single()[UsersTable.twoFactorFailures]
            if (failures >= maxFailures) {
                UsersTable.update({ UsersTable.id eq id }) {
                    it[twoFactorFailures] = 0
                    it[twoFactorLockedUntil] = OffsetDateTime.now().plusMinutes(lockMinutes)
                }
            }
        }
    }

    suspend fun resetTwoFactorFailures(id: UUID) {
        db.query {
            UsersTable.update({ UsersTable.id eq id }) {
                it[twoFactorFailures] = 0
                it[twoFactorLockedUntil] = null
            }
        }
    }

    private suspend fun update(id: UUID, changes: UsersTable.(org.jetbrains.exposed.v1.core.statements.UpdateStatement) -> Unit): User? =
        db.query {
            UsersTable.update({ UsersTable.id eq id }) {
                changes(it)
                it[updatedAt] = OffsetDateTime.now()
            }
            UsersTable.selectAll().where { UsersTable.id eq id }.singleOrNull()?.toUser()
        }

    private fun ResultRow.toUser() = User(
        id = this[UsersTable.id].value,
        email = this[UsersTable.email],
        passwordHash = this[UsersTable.passwordHash],
        displayName = this[UsersTable.displayName],
        emailVerified = this[UsersTable.emailVerified],
        createdAt = this[UsersTable.createdAt],
        totpSecret = this[UsersTable.totpSecret],
        totpPendingSecret = this[UsersTable.totpPendingSecret],
        totpLastStep = this[UsersTable.totpLastStep],
        twoFactorLockedUntil = this[UsersTable.twoFactorLockedUntil],
        avatarColor = AvatarColor.fromKey(this[UsersTable.avatarColor]),
        avatarUpdatedAt = this[UsersTable.avatarUpdatedAt],
    )
}
