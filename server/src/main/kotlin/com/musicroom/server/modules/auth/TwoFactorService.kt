package com.musicroom.server.modules.auth

import com.musicroom.server.common.exceptions.BadRequestException
import com.musicroom.server.common.exceptions.ConflictException
import com.musicroom.server.common.exceptions.HttpException
import com.musicroom.server.common.exceptions.NotFoundException
import com.musicroom.server.common.i18n.Language
import com.musicroom.server.common.security.SecretBox
import com.musicroom.server.common.security.Sha256
import com.musicroom.server.common.security.Totp
import com.musicroom.server.modules.auth.entities.EmailCodePurpose
import com.musicroom.server.modules.users.UsersRepository
import com.musicroom.server.modules.users.entities.User
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.dto.RecoveryCodesResponse
import com.musicroom.shared.dto.TwoFactorMethod
import com.musicroom.shared.dto.TwoFactorSetupResponse
import io.ktor.http.HttpStatusCode
import java.security.SecureRandom
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Two-factor authentication, off until the user turns it on, with one of two methods:
 * - an authenticator app (TOTP), plus single-use recovery codes;
 * - a code emailed at each login.
 * After [MAX_FAILURES] wrong codes in a row, codes are refused for [LOCK_MINUTES].
 */
class TwoFactorService(
    private val users: UsersRepository,
    private val recoveryCodes: RecoveryCodesRepository,
    private val secretBox: SecretBox,
    private val mailer: AccountMailer,
    private val emailCodes: EmailCodes,
) {
    private val random = SecureRandom()

    suspend fun setup(userId: UUID): TwoFactorSetupResponse {
        val user = find(userId)
        if (user.twoFactorEnabled) throw ConflictException("Two-factor authentication is already on", ApiErrors.TWO_FACTOR_ALREADY_ENABLED)
        val secret = Totp.generateSecret()
        users.setPendingTotpSecret(userId, secretBox.encrypt(secret))
        return TwoFactorSetupResponse(secret, Totp.uri(secret, user.email, ISSUER))
    }

    /** Confirms the pending secret with a first code, then returns fresh recovery codes (shown once). */
    suspend fun enable(userId: UUID, code: String, language: Language): RecoveryCodesResponse {
        val user = find(userId)
        if (user.twoFactorEnabled) throw ConflictException("Two-factor authentication is already on", ApiErrors.TWO_FACTOR_ALREADY_ENABLED)
        val pending = user.totpPendingSecret
            ?: throw BadRequestException("Start the setup first", ApiErrors.TWO_FACTOR_SETUP_REQUIRED)
        val step = Totp.verify(secretBox.decrypt(pending), code.trim(), Instant.now().epochSecond, lastUsedStep = null)
            ?: throw invalidCode()
        val updated = users.enableTotp(userId, pending, step) ?: throw NotFoundException("User not found")
        val codes = List(RECOVERY_CODE_COUNT) { newRecoveryCode() }
        recoveryCodes.replaceAll(userId, codes.map { Sha256.hex(normalizeRecoveryCode(it)) })
        mailer.notifyTwoFactorChanged(updated, enabled = true, language)
        return RecoveryCodesResponse(codes, updated.toResponse())
    }

    /**
     * Emails a code: to turn email 2FA on (when 2FA is off), or to turn it off (when it is on by email).
     * Refused when the app method is on: its codes come from the app.
     */
    suspend fun sendEmailCode(userId: UUID, language: Language) {
        val user = find(userId)
        if (user.twoFactorMethod == TwoFactorMethod.APP) {
            throw ConflictException("Two-factor authentication is already on", ApiErrors.TWO_FACTOR_ALREADY_ENABLED)
        }
        if (!emailCodes.canSendAgain(userId, EmailCodePurpose.TWO_FACTOR)) throw resendTooSoon()
        sendCode(user, language)
    }

    /** Turns email 2FA on with a code from [sendEmailCode]. The code also proves the email belongs to the user. */
    suspend fun enableEmail(userId: UUID, code: String, language: Language): User {
        val user = find(userId)
        if (user.twoFactorEnabled) throw ConflictException("Two-factor authentication is already on", ApiErrors.TWO_FACTOR_ALREADY_ENABLED)
        emailCodes.consume(userId, EmailCodePurpose.TWO_FACTOR, code)
        if (!user.emailVerified) users.markEmailVerified(userId)
        val updated = users.enableEmailTwoFactor(userId) ?: throw NotFoundException("User not found")
        mailer.notifyTwoFactorChanged(updated, enabled = true, language, byEmail = true)
        return updated
    }

    /**
     * Login with email 2FA: emails a code. Nothing is sent if a code from less than a minute ago still works,
     * so logging in twice in a row does not send two emails.
     */
    suspend fun sendLoginCode(user: User, language: Language) {
        if (emailCodes.hasActive(user.id, EmailCodePurpose.TWO_FACTOR) &&
            !emailCodes.canSendAgain(user.id, EmailCodePurpose.TWO_FACTOR)
        ) {
            return
        }
        sendCode(user, language)
    }

    /** "Send a new code" on the login screen. */
    suspend fun resendLoginCode(user: User, language: Language) {
        if (user.twoFactorMethod != TwoFactorMethod.EMAIL) {
            throw BadRequestException("This account does not use email codes", ApiErrors.TWO_FACTOR_NOT_ENABLED)
        }
        if (!emailCodes.canSendAgain(user.id, EmailCodePurpose.TWO_FACTOR)) throw resendTooSoon()
        sendCode(user, language)
    }

    /** Turning 2FA off needs a valid code too, so a stolen session alone cannot remove it. */
    suspend fun disable(userId: UUID, code: String, language: Language): User {
        val user = find(userId)
        if (!user.twoFactorEnabled) throw BadRequestException("Two-factor authentication is off", ApiErrors.TWO_FACTOR_NOT_ENABLED)
        checkCode(user, code)
        recoveryCodes.deleteAll(userId)
        val updated = users.disableTwoFactor(userId) ?: throw NotFoundException("User not found")
        mailer.notifyTwoFactorChanged(updated, enabled = false, language)
        return updated
    }

    /** Second step of a login: a code from the app, a recovery code, or the emailed code. Throws if it is wrong. */
    suspend fun checkCode(user: User, rawCode: String) {
        val lockedUntil = user.twoFactorLockedUntil
        if (lockedUntil != null && lockedUntil.isAfter(OffsetDateTime.now())) {
            throw HttpException(HttpStatusCode.TooManyRequests, ApiErrors.TWO_FACTOR_LOCKED, "Too many wrong codes. Try again in 15 minutes.")
        }
        val valid = when (user.twoFactorMethod) {
            TwoFactorMethod.APP -> checkAppCode(user, rawCode.trim())
            TwoFactorMethod.EMAIL -> checkEmailCode(user, rawCode)
            null -> throw BadRequestException("Two-factor authentication is off", ApiErrors.TWO_FACTOR_NOT_ENABLED)
        }
        if (!valid) {
            users.registerTwoFactorFailure(user.id, MAX_FAILURES, LOCK_MINUTES)
            throw invalidCode()
        }
        users.resetTwoFactorFailures(user.id)
    }

    private suspend fun checkAppCode(user: User, code: String): Boolean {
        val secret = user.totpSecret ?: return false
        return if (code.length == Totp.DIGITS && code.all { it.isDigit() }) {
            val step = Totp.verify(secretBox.decrypt(secret), code, Instant.now().epochSecond, user.totpLastStep)
            step != null && users.recordTotpStep(user.id, step)
        } else {
            recoveryCodes.consume(user.id, Sha256.hex(normalizeRecoveryCode(code)))
        }
    }

    /** A wrong code counts toward the lock; an expired code or too many tries keep their own error. */
    private suspend fun checkEmailCode(user: User, code: String): Boolean = try {
        emailCodes.consume(user.id, EmailCodePurpose.TWO_FACTOR, code)
        true
    } catch (e: HttpException) {
        if (e.error == ApiErrors.INVALID_CODE) false else throw e
    }

    private suspend fun sendCode(user: User, language: Language) {
        val code = emailCodes.create(user.id, EmailCodePurpose.TWO_FACTOR)
        mailer.sendTwoFactorCode(user, code, EmailCodes.TTL_MINUTES, language)
    }

    private fun resendTooSoon() =
        HttpException(HttpStatusCode.TooManyRequests, ApiErrors.RESEND_TOO_SOON, "Please wait a minute before asking for a new code")

    private suspend fun find(userId: UUID) = users.findById(userId) ?: throw NotFoundException("User not found")

    private fun invalidCode() = BadRequestException("Wrong code", ApiErrors.INVALID_TWO_FACTOR_CODE)

    /** 10 characters without look-alikes (0/O, 1/I), shown as XXXXX-XXXXX. */
    private fun newRecoveryCode(): String {
        val chars = (1..10).map { RECOVERY_ALPHABET[random.nextInt(RECOVERY_ALPHABET.length)] }.joinToString("")
        return chars.take(5) + "-" + chars.drop(5)
    }

    private fun normalizeRecoveryCode(code: String) = code.uppercase().filter { it.isLetterOrDigit() }

    private companion object {
        const val ISSUER = "Music Room"
        const val RECOVERY_CODE_COUNT = 10
        const val RECOVERY_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        const val MAX_FAILURES = 5
        const val LOCK_MINUTES = 15L
    }
}
