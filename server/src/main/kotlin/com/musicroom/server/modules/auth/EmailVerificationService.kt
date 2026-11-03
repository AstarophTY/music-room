package com.musicroom.server.modules.auth

import com.musicroom.server.common.exceptions.BadRequestException
import com.musicroom.server.common.exceptions.HttpException
import com.musicroom.server.common.exceptions.NotFoundException
import com.musicroom.server.common.i18n.Language
import com.musicroom.server.modules.auth.entities.EmailCodePurpose
import com.musicroom.server.modules.users.UsersRepository
import com.musicroom.server.modules.users.entities.User
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.dto.VerifyEmailRequest
import io.ktor.http.HttpStatusCode
import org.slf4j.LoggerFactory
import java.util.UUID

/** Email ownership check: a code is emailed to the user, who types it in the app. */
class EmailVerificationService(
    private val users: UsersRepository,
    private val codes: EmailCodes,
    private val mailer: AccountMailer,
) {
    private val log = LoggerFactory.getLogger(EmailVerificationService::class.java)

    /** Sends a code right after sign-up. A mail failure does not block the sign-up: the user can ask again. */
    suspend fun sendAfterSignUp(user: User, language: Language) {
        try {
            sendCode(user, language)
        } catch (e: HttpException) {
            log.warn("Verification email not sent after sign-up for user {}", user.id)
        }
    }

    suspend fun resend(userId: UUID, language: Language) {
        val user = users.findById(userId) ?: throw NotFoundException("User not found")
        if (user.emailVerified) {
            throw BadRequestException("Your email is already verified", ApiErrors.EMAIL_ALREADY_VERIFIED)
        }
        if (!codes.canSendAgain(userId, EmailCodePurpose.VERIFY_EMAIL)) {
            throw HttpException(
                HttpStatusCode.TooManyRequests,
                ApiErrors.RESEND_TOO_SOON,
                "Please wait a minute before asking for a new code",
            )
        }
        sendCode(user, language)
    }

    suspend fun verify(userId: UUID, request: VerifyEmailRequest): User {
        val user = users.findById(userId) ?: throw NotFoundException("User not found")
        if (user.emailVerified) return user
        codes.consume(userId, EmailCodePurpose.VERIFY_EMAIL, request.code)
        users.markEmailVerified(userId)
        return user.copy(emailVerified = true)
    }

    private suspend fun sendCode(user: User, language: Language) {
        val code = codes.create(user.id, EmailCodePurpose.VERIFY_EMAIL)
        mailer.sendVerificationCode(user, code, EmailCodes.TTL_MINUTES, language)
    }
}
