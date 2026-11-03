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
import com.musicroom.shared.dto.ChangePasswordRequest
import com.musicroom.shared.dto.LoginRequest
import com.musicroom.shared.dto.RefreshTokenRequest
import com.musicroom.shared.dto.RegisterRequest
import com.musicroom.shared.dto.UpdateProfileRequest
import com.musicroom.shared.dto.UserResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@Testcontainers(disabledWithoutDocker = true)
class AuthControllerTest {

    private val config = AppConfig(
        version = "test",
        database = DatabaseConfig("unused", "unused", "unused"),
        jwt = JwtConfig(secret = "x".repeat(32), issuer = "test", audience = "test", realm = "test"),
    )
    private val appModule = AppModule(
        config,
        TestDatabase.module,
        PasswordHasher(cost = 4),
        emailSender = { _, _, _ -> },
    )

    private fun ApplicationTestBuilder.client(): HttpClient {
        environment { config = MapApplicationConfig() }
        application { bootstrap(appModule) }
        return createClient { install(ContentNegotiation) { json(MusicRoomJson) } }
    }

    private suspend inline fun <reified T : Any> HttpClient.postJson(path: String, body: T, token: String? = null) =
        post(path) {
            contentType(ContentType.Application.Json)
            setBody(body)
            token?.let { bearerAuth(it) }
        }

    private fun newEmail() = "user-${UUID.randomUUID()}@example.com"

    private suspend fun HttpClient.register(
        email: String = newEmail(),
        password: String = PASSWORD,
        acceptedTerms: Boolean = true,
    ): HttpResponse = postJson(ApiRoutes.AUTH_REGISTER, RegisterRequest(email, password, "Alice", acceptedTerms))

    @Test
    fun `register returns tokens and stores a bcrypt hash`() = testApplication {
        val client = client()
        val email = newEmail()

        val response = client.register(email)

        assertEquals(HttpStatusCode.Created, response.status)
        val auth = response.body<AuthResponse>()
        assertEquals(email, auth.user.email)
        assertEquals("Alice", auth.user.displayName)
        val stored = appModule.users.repository.findByEmail(email)!!.passwordHash!!
        assertTrue(stored.startsWith("\$2a\$"))
        assertNotEquals(PASSWORD, stored)
    }

    @Test
    fun `register requires accepting the terms`() = testApplication {
        val client = client()
        val email = newEmail()

        assertEquals(HttpStatusCode.BadRequest, client.register(email, acceptedTerms = false).status)
        assertEquals(null, appModule.users.repository.findByEmail(email))
    }

    @Test
    fun `register normalizes email and rejects duplicates`() = testApplication {
        val client = client()
        val email = newEmail()
        client.register(email)

        assertEquals(HttpStatusCode.Conflict, client.register(email.uppercase()).status)
    }

    @Test
    fun `register rejects weak password and invalid email`() = testApplication {
        val client = client()
        assertEquals(HttpStatusCode.BadRequest, client.register(password = "short").status)
        assertEquals(HttpStatusCode.BadRequest, client.register(email = "not-an-email").status)
    }

    @Test
    fun `login works with the right password only`() = testApplication {
        val client = client()
        val email = newEmail()
        client.register(email)

        val ok = client.postJson(ApiRoutes.AUTH_LOGIN, LoginRequest(email, PASSWORD))
        assertEquals(HttpStatusCode.OK, ok.status)

        val access = ok.body<AuthResponse>().accessToken
        val me = client.get(ApiRoutes.USERS_ME) { bearerAuth(access) }.body<UserResponse>()
        assertEquals(email, me.email)

        assertEquals(
            HttpStatusCode.Unauthorized,
            client.postJson(ApiRoutes.AUTH_LOGIN, LoginRequest(email, "wrong-password1")).status,
        )
        assertEquals(
            HttpStatusCode.Unauthorized,
            client.postJson(ApiRoutes.AUTH_LOGIN, LoginRequest(newEmail(), PASSWORD)).status,
        )
    }

    @Test
    fun `refresh token rotates and reuse revokes every session`() = testApplication {
        val client = client()
        val first = client.register().body<AuthResponse>()

        val second = client.postJson(ApiRoutes.AUTH_REFRESH, RefreshTokenRequest(first.refreshToken))
        assertEquals(HttpStatusCode.OK, second.status)
        val secondTokens = second.body<AuthResponse>()

        // The old token was already used: it is rejected and the newer one is revoked too.
        assertEquals(
            HttpStatusCode.Unauthorized,
            client.postJson(ApiRoutes.AUTH_REFRESH, RefreshTokenRequest(first.refreshToken)).status,
        )
        assertEquals(
            HttpStatusCode.Unauthorized,
            client.postJson(ApiRoutes.AUTH_REFRESH, RefreshTokenRequest(secondTokens.refreshToken)).status,
        )
    }

    @Test
    fun `logout revokes the refresh token`() = testApplication {
        val client = client()
        val auth = client.register().body<AuthResponse>()

        assertEquals(
            HttpStatusCode.NoContent,
            client.postJson(ApiRoutes.AUTH_LOGOUT, RefreshTokenRequest(auth.refreshToken)).status,
        )
        assertEquals(
            HttpStatusCode.Unauthorized,
            client.postJson(ApiRoutes.AUTH_REFRESH, RefreshTokenRequest(auth.refreshToken)).status,
        )
    }

    @Test
    fun `change password checks the current one and signs out other sessions`() = testApplication {
        val client = client()
        val email = newEmail()
        val auth = client.register(email).body<AuthResponse>()
        val newPassword = "new-password-42"

        val wrong = client.postJson(
            ApiRoutes.AUTH_PASSWORD, ChangePasswordRequest("wrong-password1", newPassword), auth.accessToken,
        )
        assertEquals(HttpStatusCode.Forbidden, wrong.status)

        val ok = client.postJson(ApiRoutes.AUTH_PASSWORD, ChangePasswordRequest(PASSWORD, newPassword), auth.accessToken)
        assertEquals(HttpStatusCode.OK, ok.status)

        assertEquals(
            HttpStatusCode.Unauthorized,
            client.postJson(ApiRoutes.AUTH_REFRESH, RefreshTokenRequest(auth.refreshToken)).status,
        )
        assertEquals(
            HttpStatusCode.Unauthorized,
            client.postJson(ApiRoutes.AUTH_LOGIN, LoginRequest(email, PASSWORD)).status,
        )
        assertEquals(HttpStatusCode.OK, client.postJson(ApiRoutes.AUTH_LOGIN, LoginRequest(email, newPassword)).status)
    }

    @Test
    fun `update profile changes the display name`() = testApplication {
        val client = client()
        val auth = client.register().body<AuthResponse>()

        val response = client.patch(ApiRoutes.USERS_ME) {
            contentType(ContentType.Application.Json)
            setBody(UpdateProfileRequest("  Bob  "))
            bearerAuth(auth.accessToken)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Bob", response.body<UserResponse>().displayName)
        assertFalse(
            client.patch(ApiRoutes.USERS_ME) {
                contentType(ContentType.Application.Json)
                setBody(UpdateProfileRequest(" "))
                bearerAuth(auth.accessToken)
            }.status.value in 200..299,
        )
    }

    private companion object {
        const val PASSWORD = "hunter42abc"
    }
}
