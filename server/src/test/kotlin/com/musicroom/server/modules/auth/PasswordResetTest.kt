package com.musicroom.server.modules.auth

import com.musicroom.server.AppModule
import com.musicroom.server.bootstrap
import com.musicroom.server.common.security.PasswordHasher
import com.musicroom.server.config.AppConfig
import com.musicroom.server.config.DatabaseConfig
import com.musicroom.server.config.JwtConfig
import com.musicroom.server.database.TestDatabase
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.ApiRoutes
import com.musicroom.shared.MusicRoomJson
import com.musicroom.shared.dto.AuthResponse
import com.musicroom.shared.dto.ChangePasswordRequest
import com.musicroom.shared.dto.ErrorResponse
import com.musicroom.shared.dto.ForgotPasswordRequest
import com.musicroom.shared.dto.LoginRequest
import com.musicroom.shared.dto.RefreshTokenRequest
import com.musicroom.shared.dto.RegisterRequest
import com.musicroom.shared.dto.ResetPasswordRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Testcontainers(disabledWithoutDocker = true)
class PasswordResetTest {

    private data class Mail(val to: String, val subject: String, val body: String)

    private val config = AppConfig(
        version = "test",
        database = DatabaseConfig("unused", "unused", "unused"),
        jwt = JwtConfig(secret = "x".repeat(32), issuer = "test", audience = "test", realm = "test"),
    )
    private val sent = CopyOnWriteArrayList<Mail>()
    private val appModule = AppModule(
        config,
        TestDatabase.module,
        PasswordHasher(cost = 4),
        emailSender = { to, subject, body -> sent += Mail(to, subject, body) },
    )

    private fun ApplicationTestBuilder.client(): HttpClient {
        environment { config = MapApplicationConfig() }
        application { bootstrap(appModule) }
        return createClient { install(ContentNegotiation) { json(MusicRoomJson) } }
    }

    private suspend inline fun <reified T : Any> HttpClient.postJson(
        path: String,
        body: T,
        token: String? = null,
        language: String = "en",
    ): HttpResponse = post(path) {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.AcceptLanguage, language)
        setBody(body)
        token?.let { bearerAuth(it) }
    }

    private fun mailsTo(email: String) = sent.filter { it.to == email }
    private fun codeIn(mail: Mail) = Regex("\\b\\d{6}\\b").find(mail.subject)!!.value

    private suspend fun HttpClient.signUp(email: String): AuthResponse =
        postJson(ApiRoutes.AUTH_REGISTER, RegisterRequest(email, OLD, "Alice", acceptedTerms = true)).body()

    @Test
    fun `reset code sets a new password, signs out other devices and sends an alert`() = testApplication {
        val client = client()
        val email = "user-${UUID.randomUUID()}@example.com"
        val session = client.signUp(email)

        assertEquals(HttpStatusCode.NoContent, client.postJson(ApiRoutes.AUTH_PASSWORD_FORGOT, ForgotPasswordRequest(email)).status)
        val code = codeIn(mailsTo(email).last())

        val wrongCode = if (code == "000000") "111111" else "000000"
        val wrong = client.postJson(ApiRoutes.AUTH_PASSWORD_RESET, ResetPasswordRequest(email, wrongCode, NEW))
        assertEquals(ApiErrors.INVALID_CODE, wrong.body<ErrorResponse>().error)

        val reset = client.postJson(ApiRoutes.AUTH_PASSWORD_RESET, ResetPasswordRequest(email, code, NEW))
        assertEquals(HttpStatusCode.OK, reset.status)
        assertTrue(reset.body<AuthResponse>().user.emailVerified)

        assertTrue(mailsTo(email).last().subject.contains("password was changed"))
        assertEquals(HttpStatusCode.Unauthorized, client.postJson(ApiRoutes.AUTH_LOGIN, LoginRequest(email, OLD)).status)
        assertEquals(HttpStatusCode.OK, client.postJson(ApiRoutes.AUTH_LOGIN, LoginRequest(email, NEW)).status)
        assertEquals(
            HttpStatusCode.Unauthorized,
            client.postJson(ApiRoutes.AUTH_REFRESH, RefreshTokenRequest(session.refreshToken)).status,
        )
        // The code works once.
        val reused = client.postJson(ApiRoutes.AUTH_PASSWORD_RESET, ResetPasswordRequest(email, code, "another-pass9"))
        assertEquals(HttpStatusCode.BadRequest, reused.status)
    }

    @Test
    fun `forgot password does not reveal unknown emails`() = testApplication {
        val client = client()
        val email = "nobody-${UUID.randomUUID()}@example.com"

        assertEquals(HttpStatusCode.NoContent, client.postJson(ApiRoutes.AUTH_PASSWORD_FORGOT, ForgotPasswordRequest(email)).status)
        assertTrue(mailsTo(email).isEmpty())
    }

    @Test
    fun `changing the password emails an alert in the user's language`() = testApplication {
        val client = client()
        val email = "user-${UUID.randomUUID()}@example.com"
        val session = client.signUp(email)

        val changed = client.postJson(
            ApiRoutes.AUTH_PASSWORD, ChangePasswordRequest(OLD, NEW), session.accessToken, language = "fr-FR,fr;q=0.9",
        )

        assertEquals(HttpStatusCode.OK, changed.status)
        assertEquals("Ton mot de passe Music Room a été modifié", mailsTo(email).last().subject)
    }

    private companion object {
        const val OLD = "hunter42abc"
        const val NEW = "new-password-42"
    }
}
