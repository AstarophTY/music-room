package com.musicroom.server.modules.auth

import com.musicroom.server.common.exceptions.HttpException
import com.musicroom.server.common.i18n.Language
import com.musicroom.server.common.mail.EmailSender
import com.musicroom.server.modules.users.entities.User
import com.musicroom.shared.ApiErrors
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import org.slf4j.LoggerFactory

/** Account emails (codes, security alerts), in the user's language. */
class AccountMailer(private val sender: EmailSender) {
    private val log = LoggerFactory.getLogger(AccountMailer::class.java)

    suspend fun sendVerificationCode(user: User, code: String, ttlMinutes: Long, language: Language) = when (language) {
        Language.FR -> send(
            user,
            subject = "Ton code Music Room : $code",
            body = """
                |Salut ${user.displayName},
                |
                |Ton code de vérification Music Room est : $code
                |
                |Il expire dans $ttlMinutes minutes. Si tu n'as pas créé de compte, ignore cet email.
            """.trimMargin(),
        )
        Language.EN -> send(
            user,
            subject = "Your Music Room code: $code",
            body = """
                |Hello ${user.displayName},
                |
                |Your Music Room verification code is: $code
                |
                |It expires in $ttlMinutes minutes. If you did not create an account, ignore this email.
            """.trimMargin(),
        )
    }

    suspend fun sendPasswordResetCode(user: User, code: String, ttlMinutes: Long, language: Language) = when (language) {
        Language.FR -> send(
            user,
            subject = "Réinitialise ton mot de passe Music Room : $code",
            body = """
                |Salut ${user.displayName},
                |
                |Pour choisir un nouveau mot de passe, entre ce code dans l'app : $code
                |
                |Il expire dans $ttlMinutes minutes. Si tu n'as rien demandé, ignore cet email : ton mot de passe ne change pas.
            """.trimMargin(),
        )
        Language.EN -> send(
            user,
            subject = "Reset your Music Room password: $code",
            body = """
                |Hello ${user.displayName},
                |
                |To choose a new password, enter this code in the app: $code
                |
                |It expires in $ttlMinutes minutes. If you did not ask for it, ignore this email: your password stays the same.
            """.trimMargin(),
        )
    }

    suspend fun sendTwoFactorCode(user: User, code: String, ttlMinutes: Long, language: Language) = when (language) {
        Language.FR -> send(
            user,
            subject = "Ton code de connexion Music Room : $code",
            body = """
                |Salut ${user.displayName},
                |
                |Ton code de double authentification Music Room est : $code
                |
                |Il expire dans $ttlMinutes minutes. Si tu n'essaies pas de te connecter, change ton mot de passe : quelqu'un le connaît.
            """.trimMargin(),
        )
        Language.EN -> send(
            user,
            subject = "Your Music Room login code: $code",
            body = """
                |Hello ${user.displayName},
                |
                |Your Music Room two-factor code is: $code
                |
                |It expires in $ttlMinutes minutes. If you are not trying to log in, change your password: someone knows it.
            """.trimMargin(),
        )
    }

    /** Security alert after a password change. Never fails the request: the change is already done. */
    suspend fun notifyPasswordChanged(user: User, language: Language) {
        try {
            when (language) {
                Language.FR -> send(
                    user,
                    subject = "Ton mot de passe Music Room a été modifié",
                    body = """
                        |Salut ${user.displayName},
                        |
                        |Le mot de passe de ton compte Music Room vient d'être modifié, et tes autres appareils ont été déconnectés.
                        |
                        |Si ce n'est pas toi, utilise tout de suite « Mot de passe oublié » dans l'app pour reprendre ton compte.
                    """.trimMargin(),
                )
                Language.EN -> send(
                    user,
                    subject = "Your Music Room password was changed",
                    body = """
                        |Hello ${user.displayName},
                        |
                        |The password of your Music Room account was just changed, and your other devices were signed out.
                        |
                        |If this was not you, use "Forgot password" in the app right away to get your account back.
                    """.trimMargin(),
                )
            }
        } catch (e: HttpException) {
            log.warn("Password change alert not sent to user {}", user.id)
        }
    }

    /** Security alert when two-factor authentication is turned on or off. Never fails the request. */
    suspend fun notifyTwoFactorChanged(user: User, enabled: Boolean, language: Language, byEmail: Boolean = false) {
        val (subject, body) = when (language) {
            Language.FR -> if (enabled) {
                "Double authentification activée sur Music Room" to
                    "Salut ${user.displayName},\n\nLa double authentification est maintenant activée sur ton compte. " +
                    (if (byEmail) "Un code te sera envoyé par email à chaque connexion." else "Garde tes codes de secours en lieu sûr.") +
                    "\n\nSi ce n'est pas toi, change ton mot de passe tout de suite."
            } else {
                "Double authentification désactivée sur Music Room" to
                    "Salut ${user.displayName},\n\nLa double authentification vient d'être désactivée sur ton compte.\n\n" +
                    "Si ce n'est pas toi, change ton mot de passe tout de suite et réactive-la."
            }
            Language.EN -> if (enabled) {
                "Two-factor authentication turned on for Music Room" to
                    "Hello ${user.displayName},\n\nTwo-factor authentication is now on for your account. " +
                    (if (byEmail) "A code will be emailed to you at each login." else "Keep your recovery codes somewhere safe.") +
                    "\n\nIf this was not you, change your password right away."
            } else {
                "Two-factor authentication turned off for Music Room" to
                    "Hello ${user.displayName},\n\nTwo-factor authentication was just turned off for your account.\n\n" +
                    "If this was not you, change your password right away and turn it back on."
            }
        }
        try {
            send(user, subject, body)
        } catch (e: HttpException) {
            log.warn("2FA alert not sent to user {}", user.id)
        }
    }

    private suspend fun send(user: User, subject: String, body: String) {
        try {
            sender.send(user.email, subject, body)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.error("Could not send an email to user {}", user.id, e)
            throw HttpException(
                HttpStatusCode.ServiceUnavailable,
                ApiErrors.EMAIL_UNAVAILABLE,
                "Could not send the email. Try again later.",
            )
        }
    }
}
