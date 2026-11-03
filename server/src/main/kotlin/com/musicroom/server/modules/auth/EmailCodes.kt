package com.musicroom.server.modules.auth

import com.musicroom.server.common.exceptions.BadRequestException
import com.musicroom.server.common.exceptions.ValidationException
import com.musicroom.server.common.security.Sha256
import com.musicroom.server.modules.auth.entities.EmailCodePurpose
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.AuthValidation
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.util.UUID

/**
 * 6-digit codes sent by email. A code is stored hashed, works once, expires after [TTL_MINUTES]
 * and allows [MAX_ATTEMPTS] tries; a new one can be asked every [RESEND_COOLDOWN_SECONDS].
 */
class EmailCodes(private val repository: EmailVerificationCodesRepository) {
    private val random = SecureRandom()

    /** Creates a code (the previous one stops working) and returns it in clear, to be emailed. */
    suspend fun create(userId: UUID, purpose: EmailCodePurpose): String {
        val code = random.nextInt(1_000_000).toString().padStart(AuthValidation.VERIFICATION_CODE_LENGTH, '0')
        repository.replace(userId, purpose, Sha256.hex(code), OffsetDateTime.now().plusMinutes(TTL_MINUTES))
        return code
    }

    suspend fun canSendAgain(userId: UUID, purpose: EmailCodePurpose): Boolean {
        val lastSentAt = repository.lastSentAt(userId, purpose) ?: return true
        return lastSentAt.isBefore(OffsetDateTime.now().minusSeconds(RESEND_COOLDOWN_SECONDS))
    }

    /** Checks the code and marks it used. Throws a 400 with a precise error code otherwise. */
    suspend fun consume(userId: UUID, purpose: EmailCodePurpose, rawCode: String) {
        val code = AuthValidation.normalizeVerificationCode(rawCode)
        AuthValidation.verificationCodeError(code)?.let { throw ValidationException(it) }
        val active = repository.findActive(userId, purpose)
            ?: throw BadRequestException("This code has expired. Ask for a new one.", ApiErrors.CODE_EXPIRED)
        if (!repository.registerAttempt(active.id, MAX_ATTEMPTS)) {
            throw BadRequestException("Too many wrong codes. Ask for a new one.", ApiErrors.TOO_MANY_ATTEMPTS)
        }
        if (!Sha256.matches(code, active.codeHash) || !repository.markUsed(active.id)) {
            throw BadRequestException("Wrong code", ApiErrors.INVALID_CODE)
        }
    }

    companion object {
        const val TTL_MINUTES = 15L
        const val MAX_ATTEMPTS = 5
        const val RESEND_COOLDOWN_SECONDS = 60L
    }
}
