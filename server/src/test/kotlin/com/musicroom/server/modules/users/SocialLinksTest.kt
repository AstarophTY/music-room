package com.musicroom.server.modules.users

import com.musicroom.server.AppModule
import com.musicroom.server.bootstrap
import com.musicroom.server.common.security.PasswordHasher
import com.musicroom.server.config.AppConfig
import com.musicroom.server.config.DatabaseConfig
import com.musicroom.server.config.JwtConfig
import com.musicroom.server.database.TestDatabase
import com.musicroom.shared.ApiRoutes
import com.musicroom.shared.MusicRoomJson
import com.musicroom.shared.ValidationError
import com.musicroom.shared.dto.AuthResponse
import com.musicroom.shared.dto.ErrorResponse
import com.musicroom.shared.dto.RegisterRequest
import com.musicroom.shared.dto.SocialLink
import com.musicroom.shared.dto.SocialNetwork
import com.musicroom.shared.dto.UpdateSocialLinksRequest
import com.musicroom.shared.dto.UserResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
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
import kotlin.test.Test
import kotlin.test.assertEquals

@Testcontainers(disabledWithoutDocker = true)
class SocialLinksTest {

    private val config = AppConfig(
        version = "test",
        database = DatabaseConfig("unused", "unused", "unused"),
        jwt = JwtConfig(secret = "x".repeat(32), issuer = "test", audience = "test", realm = "test"),
    )
    private val appModule = AppModule(config, TestDatabase.module, PasswordHasher(cost = 4), emailSender = { _, _, _ -> })

    private fun ApplicationTestBuilder.client(): HttpClient {
        environment { config = MapApplicationConfig() }
        application { bootstrap(appModule) }
        return createClient { install(ContentNegotiation) { json(MusicRoomJson) } }
    }

    private suspend fun HttpClient.signUp(): AuthResponse = post(ApiRoutes.AUTH_REGISTER) {
        contentType(ContentType.Application.Json)
        setBody(RegisterRequest("user-${UUID.randomUUID()}@example.com", "hunter42abc", "Alice", acceptedTerms = true))
    }.body()

    private suspend fun HttpClient.putLinks(token: String, vararg links: SocialLink) = put(ApiRoutes.USERS_ME_SOCIAL_LINKS) {
        bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody(UpdateSocialLinksRequest(links.toList()))
    }

    @Test
    fun `links are cleaned, saved and returned with the profile`() = testApplication {
        val client = client()
        val auth = client.signUp()

        val saved = client.putLinks(
            auth.accessToken,
            SocialLink(SocialNetwork.DISCORD, "Alice_42"),
            SocialLink(SocialNetwork.INSTAGRAM, "https://www.instagram.com/alice.music/?igsh=x"),
            SocialLink(SocialNetwork.X, ""),
        )
        assertEquals(HttpStatusCode.OK, saved.status)
        val expected = listOf(SocialLink(SocialNetwork.INSTAGRAM, "alice.music"), SocialLink(SocialNetwork.DISCORD, "alice_42"))
        assertEquals(expected, saved.body<UserResponse>().socialLinks)
        assertEquals(expected, client.get(ApiRoutes.USERS_ME) { bearerAuth(auth.accessToken) }.body<UserResponse>().socialLinks)

        // Sending the list again replaces it.
        assertEquals(emptyList(), client.putLinks(auth.accessToken).body<UserResponse>().socialLinks)
    }

    @Test
    fun `invalid usernames are refused`() = testApplication {
        val client = client()
        val auth = client.signUp()
        val response = client.putLinks(auth.accessToken, SocialLink(SocialNetwork.TWITCH, "a b"))
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(ValidationError.SOCIAL_HANDLE_INVALID.code, response.body<ErrorResponse>().error)
    }
}
