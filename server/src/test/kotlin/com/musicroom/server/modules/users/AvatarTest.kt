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
import com.musicroom.shared.dto.AuthResponse
import com.musicroom.shared.dto.AvatarColor
import com.musicroom.shared.dto.ErrorResponse
import com.musicroom.shared.dto.RegisterRequest
import com.musicroom.shared.dto.UpdateProfileRequest
import com.musicroom.shared.dto.UserResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
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
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Testcontainers(disabledWithoutDocker = true)
class AvatarTest {

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
        setBody(RegisterRequest("user-${UUID.randomUUID()}@example.com", "hunter42abc", "Alice ${UUID.randomUUID().toString().take(8)}", acceptedTerms = true))
    }.body()

    private fun png(width: Int, height: Int): ByteArray =
        ByteArrayOutputStream().also { ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", it) }.toByteArray()

    @Test
    fun `photo is cropped to a 256 square JPEG and can be removed`() = testApplication {
        val client = client()
        val auth = client.signUp()

        val uploaded = client.put(ApiRoutes.USERS_ME_AVATAR) {
            bearerAuth(auth.accessToken)
            contentType(ContentType.Image.PNG)
            setBody(png(800, 400))
        }
        assertEquals(HttpStatusCode.OK, uploaded.status)
        assertNotNull(uploaded.body<UserResponse>().avatarVersion)

        val image = client.get(ApiRoutes.userAvatar(auth.user.id)) { bearerAuth(auth.accessToken) }
        assertEquals(ContentType.Image.JPEG, image.headers["Content-Type"]?.let(ContentType::parse))
        val decoded = ImageIO.read(ByteArrayInputStream(image.body<ByteArray>()))
        assertEquals(256, decoded.width)
        assertEquals(256, decoded.height)

        val removed = client.delete(ApiRoutes.USERS_ME_AVATAR) { bearerAuth(auth.accessToken) }
        assertNull(removed.body<UserResponse>().avatarVersion)
        assertEquals(HttpStatusCode.NotFound, client.get(ApiRoutes.userAvatar(auth.user.id)) { bearerAuth(auth.accessToken) }.status)
    }

    @Test
    fun `files that are not images are refused`() = testApplication {
        val client = client()
        val auth = client.signUp()
        val response = client.put(ApiRoutes.USERS_ME_AVATAR) {
            bearerAuth(auth.accessToken)
            contentType(ContentType.Image.PNG)
            setBody("<script>alert(1)</script>".toByteArray())
        }
        assertEquals(ApiErrors.INVALID_IMAGE, response.body<ErrorResponse>().error)
    }

    @Test
    fun `avatar color can be changed`() = testApplication {
        val client = client()
        val auth = client.signUp()
        val response = client.patch(ApiRoutes.USERS_ME) {
            bearerAuth(auth.accessToken)
            contentType(ContentType.Application.Json)
            setBody(UpdateProfileRequest(avatarColor = AvatarColor.IRIS))
        }
        assertEquals(AvatarColor.IRIS, response.body<UserResponse>().avatarColor)
        assertTrue(response.body<UserResponse>().displayName.startsWith("Alice "))
    }
}
