package com.musicroom.server.modules.auth

import com.musicroom.server.common.Controller
import com.musicroom.server.common.Module
import com.musicroom.server.common.jwt.JwtService
import com.musicroom.server.common.mail.EmailSender
import com.musicroom.server.common.oauth.OAuthTokenVerifier
import com.musicroom.server.common.security.PasswordHasher
import com.musicroom.server.common.security.SecretBox
import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.users.UsersRepository

class AuthModule(
    database: DatabaseModule,
    users: UsersRepository,
    jwtService: JwtService,
    passwordHasher: PasswordHasher,
    emailSender: EmailSender,
    oauthVerifier: OAuthTokenVerifier,
    secretBox: SecretBox,
    refreshTokenTtlDays: Long,
) : Module {
    val repository = RefreshTokensRepository(database)
    val oauthAccounts = OAuthAccountsRepository(database)
    private val emailCodes = EmailCodes(EmailVerificationCodesRepository(database))
    private val mailer = AccountMailer(emailSender)
    val emailVerification = EmailVerificationService(users, emailCodes, mailer)
    val twoFactor = TwoFactorService(users, RecoveryCodesRepository(database), secretBox, mailer)
    val service = AuthService(
        users = users,
        refreshTokens = repository,
        oauthAccounts = oauthAccounts,
        emailVerification = emailVerification,
        emailCodes = emailCodes,
        mailer = mailer,
        twoFactor = twoFactor,
        oauthVerifier = oauthVerifier,
        jwt = jwtService,
        passwordHasher = passwordHasher,
        refreshTokenTtlDays = refreshTokenTtlDays,
    )

    override val controllers: List<Controller> = listOf(AuthController(service, emailVerification, twoFactor))
}
