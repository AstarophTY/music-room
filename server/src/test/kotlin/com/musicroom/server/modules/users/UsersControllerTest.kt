package com.musicroom.server.modules.users

import com.musicroom.server.AppModule
import com.musicroom.server.bootstrap
import com.musicroom.server.config.AppConfig
import com.musicroom.server.config.DatabaseConfig
import com.musicroom.server.config.JwtConfig
import com.musicroom.server.database.TestDatabase
import com.musicroom.shared.ApiRoutes
import com.musicroom.shared.MusicRoomJson
import com.musicroom.shared.dto.UserResponse
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

@Testcontainers(disabledWithoutDocker = true)
class UsersControllerTest {

    private val config = AppConfig(
        version = "test",
        database = DatabaseConfig("unused", "unused", "unused"),
        jwt = JwtConfig(secret = "x".repeat(32), issuer = "test", audience = "test", realm = "test"),
    )
    private val appModule = AppModule(config, TestDatabase.module)
    private val users = UsersModule(TestDatabase.module)

    private fun ApplicationTestBuilder.setup() {
        environment { config = MapApplicationConfig() }
        application { bootstrap(appModule) }
    }

    private fun ApplicationTestBuilder.jsonClient() = createClient {
        install(ContentNegotiation) { json(MusicRoomJson) }
    }

    @Test
    fun `me without token returns 401`() = testApplication {
        setup()
        assertEquals(HttpStatusCode.Unauthorized, jsonClient().get(ApiRoutes.USERS_ME).status)
    }

    @Test
    fun `me returns the authenticated user`() = testApplication {
        setup()
        val user = runBlocking {
            users.repository.create("alice-${UUID.randomUUID()}@example.com", "Alice", passwordHash = null)
        }
        val token = appModule.jwtService.createAccessToken(user.id)

        val response = jsonClient().get(ApiRoutes.USERS_ME) { bearerAuth(token) }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(user.toResponse(), response.body<UserResponse>())
    }

    @Test
    fun `me with token of unknown user returns 404`() = testApplication {
        setup()
        val token = appModule.jwtService.createAccessToken(UUID.randomUUID())
        assertEquals(HttpStatusCode.NotFound, jsonClient().get(ApiRoutes.USERS_ME) { bearerAuth(token) }.status)
    }
}
