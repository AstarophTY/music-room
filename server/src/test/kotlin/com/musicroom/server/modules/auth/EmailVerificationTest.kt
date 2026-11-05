package com.musicroom.server.modules.auth

import com.musicroom.server.AppModule
import com.musicroom.server.bootstrap
import com.musicroom.server.common.security.PasswordHasher
import com.musicroom.server.config.AppConfig
import com.musicroom.server.config.DatabaseConfig
import com.musicroom.server.config.JwtConfig
import com.musicroom.server.database.TestDatabase
import com.musicroom.shared.ApiRoutes
import com.musicroom.shared.MusicRoomJson
import com.musicroom.shared.dto.AuthResponse
import com.musicroom.shared.dto.RegisterRequest
import com.musicroom.shared.dto.UserResponse
import com.musicroom.shared.dto.VerifyEmailRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@Testcontainers(disabledWithoutDocker = true)
class EmailVerificationTest {

    private val config = AppConfig(
        version = "test",
        database = DatabaseConfig("unused", "unused", "unused"),
        jwt = JwtConfig(secret = "x".repeat(32), issuer = "test", audience = "test", realm = "test"),
    )

    /** Last email body sent to each address. */
    private val inbox = ConcurrentHashMap<String, String>()
    private val appModule = AppModule(
        config,
        TestDatabase.module,
        PasswordHasher(cost = 4),
        emailSender = { to, _, body -> inbox[to] = body },
    )

    private fun ApplicationTestBuilder.client(): HttpClient {
        environment { config = MapApplicationConfig() }
        application { bootstrap(appModule) }
        return createClient { install(ContentNegotiation) { json(MusicRoomJson) } }
    }

    private fun codeSentTo(email: String): String = Regex("\\b\\d{6}\\b").find(inbox.getValue(email))!!.value

    private suspend fun HttpClient.signUp(email: String): AuthResponse = post(ApiRoutes.AUTH_REGISTER) {
        contentType(ContentType.Application.Json)
        setBody(RegisterRequest(email, "hunter42abc", "Alice", acceptedTerms = true))
    }.body()

    private suspend fun HttpClient.verify(token: String, code: String) = post(ApiRoutes.AUTH_EMAIL_VERIFY) {
        contentType(ContentType.Application.Json)
        setBody(VerifyEmailRequest(code))
        bearerAuth(token)
    }

    private fun otherCode(code: String) = ((code.toInt() + 1) % 1_000_000).toString().padStart(6, '0')

    @Test
    fun `sign-up emails a code that verifies the address once`() = testApplication {
        val client = client()
        val email = "user-${UUID.randomUUID()}@example.com"
        val auth = client.signUp(email)
        assertFalse(auth.user.emailVerified)

        val code = codeSentTo(email)
        assertEquals(HttpStatusCode.BadRequest, client.verify(auth.accessToken, otherCode(code)).status)

        val ok = client.verify(auth.accessToken, code)
        assertEquals(HttpStatusCode.OK, ok.status)
        assertTrue(ok.body<UserResponse>().emailVerified)
        assertTrue(appModule.users.repository.findByEmail(email)!!.emailVerified)
    }

    @Test
    fun `code is locked after too many wrong tries`() = testApplication {
        val client = client()
        val email = "user-${UUID.randomUUID()}@example.com"
        val auth = client.signUp(email)
        val code = codeSentTo(email)

        repeat(5) { assertEquals(HttpStatusCode.BadRequest, client.verify(auth.accessToken, otherCode(code)).status) }

        assertEquals(HttpStatusCode.BadRequest, client.verify(auth.accessToken, code).status)
        assertFalse(appModule.users.repository.findByEmail(email)!!.emailVerified)
    }

    @Test
    fun `resend replaces the code and is limited to one per minute`() = testApplication {
        val client = client()
        val email = "user-${UUID.randomUUID()}@example.com"
        val auth = client.signUp(email)
        val firstCode = codeSentTo(email)

        val resend = client.post(ApiRoutes.AUTH_EMAIL_RESEND) { bearerAuth(auth.accessToken) }
        assertEquals(HttpStatusCode.TooManyRequests, resend.status)
        assertEquals(firstCode, codeSentTo(email))

        // Pretend the first code was sent long ago so a new one can be requested.
        TestDatabase.module.query {
            exec(
                "UPDATE email_verification_codes SET created_at = now() - interval '2 minutes' " +
                    "WHERE user_id = '${auth.user.id}'",
            )
        }
        assertEquals(
            HttpStatusCode.NoContent,
            client.post(ApiRoutes.AUTH_EMAIL_RESEND) { bearerAuth(auth.accessToken) }.status,
        )
        val secondCode = codeSentTo(email)
        assertNotEquals(firstCode, secondCode)
        assertEquals(HttpStatusCode.BadRequest, client.verify(auth.accessToken, firstCode).status)
        assertEquals(HttpStatusCode.OK, client.verify(auth.accessToken, secondCode).status)
    }
}
