package com.musicroom.server.modules.auth

import com.musicroom.server.AppModule
import com.musicroom.server.bootstrap
import com.musicroom.server.common.exceptions.UnauthorizedException
import com.musicroom.server.common.oauth.OAuthIdentity
import com.musicroom.server.common.security.PasswordHasher
import com.musicroom.server.config.AppConfig
import com.musicroom.server.config.DatabaseConfig
import com.musicroom.server.config.JwtConfig
import com.musicroom.server.database.TestDatabase
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.ApiRoutes
import com.musicroom.shared.MusicRoomJson
import com.musicroom.shared.dto.AuthResponse
import com.musicroom.shared.dto.ErrorResponse
import com.musicroom.shared.dto.LoginRequest
import com.musicroom.shared.dto.OAuthLoginRequest
import com.musicroom.shared.dto.OAuthProvider
import com.musicroom.shared.dto.RefreshTokenRequest
import com.musicroom.shared.dto.RegisterRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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
import kotlin.test.assertTrue

@Testcontainers(disabledWithoutDocker = true)
class OAuthLoginTest {

    private val config = AppConfig(
        version = "test",
        database = DatabaseConfig("unused", "unused", "unused"),
        jwt = JwtConfig(secret = "x".repeat(32), issuer = "test", audience = "test", realm = "test"),
    )

    /** Fake provider: the "ID token" is "<subject>|<email>", and "bad" is a forged token. */
    private val appModule = AppModule(
        config,
        TestDatabase.module,
        PasswordHasher(cost = 4),
        emailSender = { _, _, _ -> },
        oauthVerifier = { provider, idToken ->
            if (idToken == "bad") throw UnauthorizedException("Invalid token")
            val (subject, email) = idToken.split('|')
            OAuthIdentity(provider, subject, email, emailVerified = true, name = "Gina Google")
        },
    )

    private fun ApplicationTestBuilder.client(): HttpClient {
        environment { config = MapApplicationConfig() }
        application { bootstrap(appModule) }
        return createClient { install(ContentNegotiation) { json(MusicRoomJson) } }
    }

    private suspend inline fun <reified T : Any> HttpClient.postJson(path: String, body: T): HttpResponse =
        post(path) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    private suspend fun HttpClient.google(token: String, acceptedTerms: Boolean = false) =
        postJson(ApiRoutes.AUTH_OAUTH, OAuthLoginRequest(OAuthProvider.GOOGLE, token, acceptedTerms))

    private fun newEmail() = "user-${UUID.randomUUID()}@example.com"

    @Test
    fun `new account needs the terms, then the same identity logs into it`() = testApplication {
        val client = client()
        val token = "sub-${UUID.randomUUID()}|${newEmail()}"

        val refused = client.google(token)
        assertEquals(HttpStatusCode.Forbidden, refused.status)
        assertEquals(ApiErrors.TERMS_REQUIRED, refused.body<ErrorResponse>().error)

        val created = client.google(token, acceptedTerms = true)
        assertEquals(HttpStatusCode.OK, created.status)
        val user = created.body<AuthResponse>().user
        assertTrue(user.emailVerified)
        assertEquals("Gina Google", user.displayName)

        val again = client.google(token)
        assertEquals(HttpStatusCode.OK, again.status)
        assertEquals(user.id, again.body<AuthResponse>().user.id)
    }

    @Test
    fun `forged token is rejected`() = testApplication {
        assertEquals(HttpStatusCode.Unauthorized, client().google("bad", acceptedTerms = true).status)
    }

    @Test
    fun `verified account with the same email is linked`() = testApplication {
        val client = client()
        val email = newEmail()
        val registered = client.postJson(
            ApiRoutes.AUTH_REGISTER, RegisterRequest(email, PASSWORD, "Alice", acceptedTerms = true),
        ).body<AuthResponse>()
        appModule.users.repository.markEmailVerified(UUID.fromString(registered.user.id))

        val linked = client.google("sub-${UUID.randomUUID()}|$email")

        assertEquals(HttpStatusCode.OK, linked.status)
        assertEquals(registered.user.id, linked.body<AuthResponse>().user.id)
        // The password still works.
        assertEquals(HttpStatusCode.OK, client.postJson(ApiRoutes.AUTH_LOGIN, LoginRequest(email, PASSWORD)).status)
    }

    @Test
    fun `unverified account with the same email loses its password`() = testApplication {
        val client = client()
        val email = newEmail()
        val squatter = client.postJson(
            ApiRoutes.AUTH_REGISTER, RegisterRequest(email, PASSWORD, "Squatter", acceptedTerms = true),
        ).body<AuthResponse>()

        val owner = client.google("sub-${UUID.randomUUID()}|$email")

        assertEquals(HttpStatusCode.OK, owner.status)
        assertTrue(owner.body<AuthResponse>().user.emailVerified)
        assertEquals(
            HttpStatusCode.Unauthorized,
            client.postJson(ApiRoutes.AUTH_LOGIN, LoginRequest(email, PASSWORD)).status,
        )
        assertEquals(
            HttpStatusCode.Unauthorized,
            client.postJson(ApiRoutes.AUTH_REFRESH, RefreshTokenRequest(squatter.refreshToken)).status,
        )
    }

    private companion object {
        const val PASSWORD = "hunter42abc"
    }
}
