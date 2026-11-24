package com.musicroom.server.modules.users

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
import com.musicroom.shared.ProfileValidation
import com.musicroom.shared.dto.AuthResponse
import com.musicroom.shared.dto.ErrorResponse
import com.musicroom.shared.dto.RegisterRequest
import com.musicroom.shared.dto.UpdateUserDetailsRequest
import com.musicroom.shared.dto.UserDetailsResponse
import com.musicroom.shared.dto.UserProfileResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers(disabledWithoutDocker = true)
class UserDetailsTest {
    private val config = AppConfig(
        version = "test",
        database = DatabaseConfig("unused", "unused", "unused"),
        jwt = JwtConfig(secret = "x".repeat(32), issuer = "test", audience = "test", realm = "test"),
    )
    private val appModule = AppModule(config, TestDatabase.module, PasswordHasher(cost = 4), emailSender = { _, _, _ -> })

    private fun ApplicationTestBuilder.api(): HttpClient {
        environment { config = MapApplicationConfig() }
        application { bootstrap(appModule) }
        return createClient { install(ContentNegotiation) { json(MusicRoomJson) } }
    }

    private suspend fun HttpClient.user(): AuthResponse = post(ApiRoutes.AUTH_REGISTER) {
        contentType(ContentType.Application.Json)
        setBody(RegisterRequest("user-${UUID.randomUUID()}@example.com", "hunter42abc", "U ${UUID.randomUUID().toString().take(8)}", acceptedTerms = true))
    }.body()

    private suspend fun HttpClient.befriend(a: AuthResponse, b: AuthResponse) {
        post(ApiRoutes.friend(b.user.id)) { bearerAuth(a.accessToken) }
        post(ApiRoutes.friend(a.user.id)) { bearerAuth(b.accessToken) }
    }

    @Test
    fun `each audience only reads its own part of the profile`() = testApplication {
        val client = api()
        val owner = client.user()
        val friend = client.user()
        val stranger = client.user()
        client.befriend(owner, friend)

        val saved: UserDetailsResponse = client.put(ApiRoutes.USERS_ME_DETAILS) {
            bearerAuth(owner.accessToken)
            contentType(ContentType.Application.Json)
            setBody(UpdateUserDetailsRequest(" Public bio ", "friends-secret", "private-secret", listOf("Jazz", " jazz", "Rock")))
        }.body()
        assertEquals("Public bio", saved.publicInfo)
        assertEquals(listOf("Jazz", "Rock"), saved.musicPreferences)
        assertEquals(saved, client.get(ApiRoutes.USERS_ME_DETAILS) { bearerAuth(owner.accessToken) }.body())

        val path = "${ApiRoutes.USERS}/${owner.user.id}"
        val seenByFriend = client.get(path) { bearerAuth(friend.accessToken) }
        assertEquals("friends-secret", seenByFriend.body<UserProfileResponse>().friendsInfo)
        assertFalse(seenByFriend.bodyAsText().contains("private-secret"))

        val seenByStranger = client.get(path) { bearerAuth(stranger.accessToken) }
        val profile: UserProfileResponse = seenByStranger.body()
        assertEquals("Public bio", profile.publicInfo)
        assertEquals(listOf("Jazz", "Rock"), profile.musicPreferences)
        assertNull(profile.friendsInfo)
        assertFalse(seenByStranger.bodyAsText().contains("friends-secret"))
        assertFalse(seenByStranger.bodyAsText().contains("private-secret"))
    }

    @Test
    fun `details over the limits are refused`() = testApplication {
        val client = api()
        val owner = client.user()
        val response = client.put(ApiRoutes.USERS_ME_DETAILS) {
            bearerAuth(owner.accessToken)
            contentType(ContentType.Application.Json)
            setBody(UpdateUserDetailsRequest(publicInfo = "a".repeat(ProfileValidation.INFO_MAX_LENGTH + 1)))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(ApiErrors.PROFILE_DETAILS_INVALID, response.body<ErrorResponse>().error)
    }
}
