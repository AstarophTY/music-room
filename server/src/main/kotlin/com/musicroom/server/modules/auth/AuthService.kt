package com.musicroom.server.modules.auth

import com.musicroom.server.common.exceptions.BadRequestException
import com.musicroom.server.common.exceptions.ConflictException
import com.musicroom.server.common.exceptions.ForbiddenException
import com.musicroom.server.common.exceptions.HttpException
import com.musicroom.server.common.exceptions.NotFoundException
import com.musicroom.server.common.exceptions.UnauthorizedException
import com.musicroom.server.common.exceptions.ValidationException
import com.musicroom.server.common.i18n.Language
import com.musicroom.server.common.jwt.JwtService
import com.musicroom.server.common.oauth.OAuthIdentity
import com.musicroom.server.common.oauth.OAuthTokenVerifier
import com.musicroom.server.common.security.PasswordHasher
import com.musicroom.server.common.security.Sha256
import com.musicroom.server.modules.auth.entities.EmailCodePurpose
import com.musicroom.server.modules.users.UsersRepository
import com.musicroom.server.modules.users.entities.User
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.AuthValidation
import com.musicroom.shared.ValidationError
import com.musicroom.shared.dto.AuthResponse
import com.musicroom.shared.dto.ChangePasswordRequest
import com.musicroom.shared.dto.ForgotPasswordRequest
import com.musicroom.shared.dto.LoginRequest
import com.musicroom.shared.dto.OAuthLoginRequest
import com.musicroom.shared.dto.RefreshTokenRequest
import com.musicroom.shared.dto.RegisterRequest
import com.musicroom.shared.dto.ResetPasswordRequest
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.util.Base64
import java.util.UUID

class AuthService(
    private val users: UsersRepository,
    private val refreshTokens: RefreshTokensRepository,
    private val oauthAccounts: OAuthAccountsRepository,
    private val emailVerification: EmailVerificationService,
    private val emailCodes: EmailCodes,
    private val mailer: AccountMailer,
    private val oauthVerifier: OAuthTokenVerifier,
    private val jwt: JwtService,
    private val passwordHasher: PasswordHasher,
    private val refreshTokenTtlDays: Long,
) {
    private val random = SecureRandom()

    suspend fun register(request: RegisterRequest, language: Language): AuthResponse {
        val email = AuthValidation.normalizeEmail(request.email)
        val displayName = request.displayName.trim()
        validate(
            AuthValidation.emailError(email),
            AuthValidation.passwordError(request.password),
            AuthValidation.displayNameError(displayName),
            AuthValidation.termsError(request.acceptedTerms),
        )
        if (users.findByEmail(email) != null) throw ConflictException(EMAIL_TAKEN, ApiErrors.EMAIL_TAKEN)

        val passwordHash = passwordHasher.hash(request.password)
        val user = createUser { users.create(email, displayName, passwordHash, termsAcceptedAt = OffsetDateTime.now()) }
        emailVerification.sendAfterSignUp(user, language)
        return issueTokens(user)
    }

    /**
     * Google / Apple sign-in. The ID token is verified here, never trusted from the app.
     * - identity already linked: log in;
     * - email matches an account: link the identity to it;
     * - otherwise: create an account, only if the user accepted the terms (else 403 terms_required).
     */
    suspend fun oauthLogin(request: OAuthLoginRequest): AuthResponse {
        val identity = oauthVerifier.verify(request.provider, request.idToken)
        oauthAccounts.findUserId(identity.provider, identity.subject)?.let { userId ->
            return issueTokens(users.findById(userId) ?: throw UnauthorizedException("Account not found", ApiErrors.INVALID_CREDENTIALS))
        }

        val email = identity.email?.let(AuthValidation::normalizeEmail)
        if (email == null || !identity.emailVerified) {
            throw BadRequestException(
                "Your ${identity.provider.displayName} account has no verified email",
                ApiErrors.OAUTH_EMAIL_UNVERIFIED,
            )
        }
        val user = users.findByEmail(email)?.let { linkToExisting(it) } ?: createOAuthUser(identity, email, request)
        try {
            oauthAccounts.link(user.id, identity.provider, identity.subject, email)
        } catch (e: ExposedSQLException) {
            // Same identity linked by a parallel request, or this account already has another identity
            // from this provider.
            if (e.sqlState != UNIQUE_VIOLATION) throw e
            if (oauthAccounts.findUserId(identity.provider, identity.subject) != user.id) {
                throw ConflictException(
                    "This account is already linked to another ${identity.provider.displayName} account",
                    ApiErrors.OAUTH_ALREADY_LINKED,
                )
            }
        }
        return issueTokens(user)
    }

    private suspend fun linkToExisting(user: User): User {
        if (user.emailVerified) return user
        users.takeOverUnverified(user.id)
        refreshTokens.revokeAllForUser(user.id)
        return user.copy(emailVerified = true, passwordHash = null)
    }

    private suspend fun createOAuthUser(identity: OAuthIdentity, email: String, request: OAuthLoginRequest): User {
        if (!request.acceptedTerms) {
            throw HttpException(
                HttpStatusCode.Forbidden,
                ApiErrors.TERMS_REQUIRED,
                "Accept the terms of use to create your account",
            )
        }
        val displayName = listOfNotNull(request.displayName, identity.name)
            .map { it.trim() }
            .firstOrNull { AuthValidation.displayNameError(it) == null }
            ?: email.substringBefore('@').take(AuthValidation.DISPLAY_NAME_MAX_LENGTH)
        return createUser {
            users.create(
                email = email,
                displayName = displayName,
                passwordHash = null,
                emailVerified = true,
                termsAcceptedAt = OffsetDateTime.now(),
            )
        }
    }

    /** Two sign-ups with the same email at the same time: the UNIQUE constraint decides. */
    private suspend fun createUser(create: suspend () -> User): User = try {
        create()
    } catch (e: ExposedSQLException) {
        if (e.sqlState == UNIQUE_VIOLATION) throw ConflictException(EMAIL_TAKEN, ApiErrors.EMAIL_TAKEN) else throw e
    }

    suspend fun login(request: LoginRequest): AuthResponse {
        val user = users.findByEmail(AuthValidation.normalizeEmail(request.email))
        // Always run bcrypt, even for unknown emails, so response time does not reveal which emails exist.
        val passwordOk = passwordHasher.verify(request.password, user?.passwordHash)
        if (user == null || !passwordOk) {
            throw UnauthorizedException("Invalid email or password", ApiErrors.INVALID_CREDENTIALS)
        }
        return issueTokens(user)
    }

    /** Swaps a refresh token for a new pair. Each refresh token works only once (rotation). */
    suspend fun refresh(request: RefreshTokenRequest): AuthResponse {
        val stored = refreshTokens.findByHash(Sha256.hex(request.refreshToken))
            ?: throw UnauthorizedException(INVALID_REFRESH)
        if (stored.revokedAt != null || !refreshTokens.revoke(stored.id)) {
            // A used token comes back: it was probably stolen. Sign out every session of this user.
            refreshTokens.revokeAllForUser(stored.userId)
            throw UnauthorizedException(INVALID_REFRESH)
        }
        if (stored.expiresAt.isBefore(OffsetDateTime.now())) throw UnauthorizedException(INVALID_REFRESH)
        val user = users.findById(stored.userId) ?: throw UnauthorizedException(INVALID_REFRESH)
        return issueTokens(user)
    }

    suspend fun logout(request: RefreshTokenRequest) {
        refreshTokens.revokeByHash(Sha256.hex(request.refreshToken))
    }

    /** Changes the password, signs out every session, emails a security alert and returns new tokens. */
    suspend fun changePassword(userId: UUID, request: ChangePasswordRequest, language: Language): AuthResponse {
        val user = users.findById(userId) ?: throw NotFoundException("User not found")
        if (!passwordHasher.verify(request.currentPassword, user.passwordHash)) {
            throw ForbiddenException("Current password is incorrect", ApiErrors.WRONG_CURRENT_PASSWORD)
        }
        validate(AuthValidation.passwordError(request.newPassword))
        return setPassword(user, request.newPassword, language)
    }

    /**
     * Emails a reset code. Always succeeds, so the answer never tells whether an account uses this email.
     * Asking again within a minute sends nothing.
     */
    suspend fun forgotPassword(request: ForgotPasswordRequest, language: Language) {
        val user = users.findByEmail(AuthValidation.normalizeEmail(request.email)) ?: return
        if (!emailCodes.canSendAgain(user.id, EmailCodePurpose.RESET_PASSWORD)) return
        val code = emailCodes.create(user.id, EmailCodePurpose.RESET_PASSWORD)
        try {
            mailer.sendPasswordResetCode(user, code, EmailCodes.TTL_MINUTES, language)
        } catch (e: HttpException) {
            // Already logged by the mailer. Answer like a success, as for unknown emails.
        }
    }

    /** Sets a new password with the emailed code. The code also proves the email belongs to the user. */
    suspend fun resetPassword(request: ResetPasswordRequest, language: Language): AuthResponse {
        validate(AuthValidation.passwordError(request.newPassword))
        val user = users.findByEmail(AuthValidation.normalizeEmail(request.email))
            ?: throw BadRequestException("This code has expired. Ask for a new one.", ApiErrors.CODE_EXPIRED)
        emailCodes.consume(user.id, EmailCodePurpose.RESET_PASSWORD, request.code)
        if (!user.emailVerified) users.markEmailVerified(user.id)
        return setPassword(user.copy(emailVerified = true), request.newPassword, language)
    }

    private suspend fun setPassword(user: User, newPassword: String, language: Language): AuthResponse {
        users.updatePasswordHash(user.id, passwordHasher.hash(newPassword))
        refreshTokens.revokeAllForUser(user.id)
        mailer.notifyPasswordChanged(user, language)
        return issueTokens(user)
    }

    private suspend fun issueTokens(user: User): AuthResponse {
        val refreshToken = ByteArray(32).also(random::nextBytes).let {
            Base64.getUrlEncoder().withoutPadding().encodeToString(it)
        }
        refreshTokens.create(user.id, Sha256.hex(refreshToken), OffsetDateTime.now().plusDays(refreshTokenTtlDays))
        return AuthResponse(
            accessToken = jwt.createAccessToken(user.id),
            refreshToken = refreshToken,
            expiresInSeconds = jwt.accessTokenTtlSeconds,
            user = user.toResponse(),
        )
    }

    private fun validate(vararg errors: ValidationError?) {
        errors.firstOrNull { it != null }?.let { throw ValidationException(it) }
    }

    private companion object {
        const val UNIQUE_VIOLATION = "23505"
        const val EMAIL_TAKEN = "An account already exists with this email"
        const val INVALID_REFRESH = "Invalid or expired refresh token"
    }
}
