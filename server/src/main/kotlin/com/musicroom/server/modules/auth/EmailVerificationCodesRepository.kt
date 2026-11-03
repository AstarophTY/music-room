package com.musicroom.server.modules.auth

import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.auth.entities.EmailCodePurpose
import com.musicroom.server.modules.auth.entities.EmailVerificationCode
import com.musicroom.server.modules.auth.entities.EmailVerificationCodesTable
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.OffsetDateTime
import java.util.UUID

/** Emailed codes (email verification, password reset), stored hashed. */
class EmailVerificationCodesRepository(private val db: DatabaseModule) {

    /** Stores a new code and cancels the previous ones for the same purpose: only the latest email works. */
    suspend fun replace(userId: UUID, purpose: EmailCodePurpose, codeHash: String, expiresAt: OffsetDateTime) {
        db.query {
            val now = OffsetDateTime.now()
            EmailVerificationCodesTable.update({
                belongsTo(userId, purpose) and EmailVerificationCodesTable.usedAt.isNull()
            }) { it[usedAt] = now }
            EmailVerificationCodesTable.insert {
                it[EmailVerificationCodesTable.userId] = userId
                it[EmailVerificationCodesTable.purpose] = purpose.key
                it[EmailVerificationCodesTable.codeHash] = codeHash
                it[EmailVerificationCodesTable.expiresAt] = expiresAt
                it[createdAt] = now
            }
        }
    }

    /** The code that can still be used: not used, not expired. */
    suspend fun findActive(userId: UUID, purpose: EmailCodePurpose): EmailVerificationCode? = db.query {
        EmailVerificationCodesTable.selectAll()
            .where {
                belongsTo(userId, purpose) and
                    EmailVerificationCodesTable.usedAt.isNull() and
                    (EmailVerificationCodesTable.expiresAt greater OffsetDateTime.now())
            }
            .orderBy(EmailVerificationCodesTable.createdAt, SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?.let { EmailVerificationCode(it[EmailVerificationCodesTable.id].value, it[EmailVerificationCodesTable.codeHash]) }
    }

    suspend fun lastSentAt(userId: UUID, purpose: EmailCodePurpose): OffsetDateTime? = db.query {
        EmailVerificationCodesTable.selectAll()
            .where { belongsTo(userId, purpose) }
            .orderBy(EmailVerificationCodesTable.createdAt, SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?.get(EmailVerificationCodesTable.createdAt)
    }

    /** Counts one try. Atomic: returns false once [maxAttempts] is reached, even with parallel requests. */
    suspend fun registerAttempt(id: UUID, maxAttempts: Int): Boolean = db.query {
        EmailVerificationCodesTable.update({
            (EmailVerificationCodesTable.id eq id) and (EmailVerificationCodesTable.attempts less maxAttempts)
        }) { it[attempts] = attempts + 1 } == 1
    }

    /** Atomic: returns false if the code was already used (two requests with the same code). */
    suspend fun markUsed(id: UUID): Boolean = db.query {
        EmailVerificationCodesTable.update({
            (EmailVerificationCodesTable.id eq id) and EmailVerificationCodesTable.usedAt.isNull()
        }) { it[usedAt] = OffsetDateTime.now() } == 1
    }

    private fun belongsTo(userId: UUID, purpose: EmailCodePurpose): Op<Boolean> =
        (EmailVerificationCodesTable.userId eq userId) and (EmailVerificationCodesTable.purpose eq purpose.key)
}
