package com.musicroom.server.modules.social

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
import com.musicroom.shared.dto.ConversationsResponse
import com.musicroom.shared.dto.ErrorResponse
import com.musicroom.shared.dto.FriendsResponse
import com.musicroom.shared.dto.FriendshipStatus
import com.musicroom.shared.dto.MessageResponse
import com.musicroom.shared.dto.MessagesResponse
import com.musicroom.shared.dto.NotificationType
import com.musicroom.shared.dto.NotificationsResponse
import com.musicroom.shared.dto.PublicUserResponse
import com.musicroom.shared.dto.RegisterRequest
import com.musicroom.shared.dto.SendMessageRequest
import com.musicroom.shared.dto.SocialCountsResponse
import com.musicroom.shared.dto.SocialEvent
import com.musicroom.shared.dto.UsersResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.withTimeout
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Testcontainers(disabledWithoutDocker = true)
class SocialTest {

    private val config = AppConfig(
        version = "test",
        database = DatabaseConfig("unused", "unused", "unused"),
        jwt = JwtConfig(secret = "x".repeat(32), issuer = "test", audience = "test", realm = "test"),
    )
    private val appModule = AppModule(config, TestDatabase.module, PasswordHasher(cost = 4), emailSender = { _, _, _ -> })

    private data class TestUser(val id: String, val name: String, val token: String)

    private fun ApplicationTestBuilder.client(): HttpClient {
        environment { config = MapApplicationConfig() }
        application { bootstrap(appModule) }
        return createClient { install(ContentNegotiation) { json(MusicRoomJson) } }
    }

    private suspend fun HttpClient.user(prefix: String): TestUser {
        val auth: AuthResponse = post(ApiRoutes.AUTH_REGISTER) {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("user-${UUID.randomUUID()}@example.com", "hunter42abc", "$prefix ${UUID.randomUUID().toString().take(8)}", acceptedTerms = true))
        }.body()
        return TestUser(auth.user.id, auth.user.displayName, auth.accessToken)
    }

    @Test
    fun `users find each other, become friends, chat and get notified`() = testApplication {
        val client = client()
        val alice = client.user("Alice")
        val bob = client.user("Bob")

        // Search by name, any case.
        val found: UsersResponse = client.get(ApiRoutes.USERS_SEARCH) {
            bearerAuth(alice.token)
            parameter("q", bob.name.uppercase())
        }.body()
        assertEquals(bob.id, found.items.first().id)
        assertEquals(FriendshipStatus.NONE, found.items.first().friendship)

        // Not friends yet: no chat.
        val refused = client.post(ApiRoutes.conversationMessages(bob.id)) {
            bearerAuth(alice.token)
            contentType(ContentType.Application.Json)
            setBody(SendMessageRequest("hi"))
        }
        assertEquals(HttpStatusCode.Forbidden, refused.status)
        assertEquals(ApiErrors.NOT_FRIENDS, refused.body<ErrorResponse>().error)

        // Alice asks, Bob gets a notification and accepts by asking back.
        val asked: PublicUserResponse = client.post(ApiRoutes.friend(bob.id)) { bearerAuth(alice.token) }.body()
        assertEquals(FriendshipStatus.REQUEST_SENT, asked.friendship)
        val bobNotifications: NotificationsResponse = client.get(ApiRoutes.NOTIFICATIONS) { bearerAuth(bob.token) }.body()
        assertEquals(NotificationType.FRIEND_REQUEST, bobNotifications.items.single().type)
        assertEquals(alice.id, bobNotifications.items.single().actor?.id)
        assertEquals(1, client.get(ApiRoutes.SOCIAL_COUNTS) { bearerAuth(bob.token) }.body<SocialCountsResponse>().pendingRequests)

        val accepted: PublicUserResponse = client.post(ApiRoutes.friend(alice.id)) { bearerAuth(bob.token) }.body()
        assertEquals(FriendshipStatus.FRIENDS, accepted.friendship)
        val aliceFriends: FriendsResponse = client.get(ApiRoutes.FRIENDS) { bearerAuth(alice.token) }.body()
        assertEquals(listOf(bob.id), aliceFriends.friends.map { it.id })
        val aliceNotifications: NotificationsResponse = client.get(ApiRoutes.NOTIFICATIONS) { bearerAuth(alice.token) }.body()
        assertEquals(NotificationType.FRIEND_ACCEPTED, aliceNotifications.items.first().type)

        // Chat: Bob sees one unread message, reads it.
        val sent: MessageResponse = client.post(ApiRoutes.conversationMessages(bob.id)) {
            bearerAuth(alice.token)
            contentType(ContentType.Application.Json)
            setBody(SendMessageRequest("  hello Bob  "))
        }.body()
        assertEquals("hello Bob", sent.body)
        val bobChats: ConversationsResponse = client.get(ApiRoutes.CONVERSATIONS) { bearerAuth(bob.token) }.body()
        assertEquals(1, bobChats.items.single().unreadCount)
        assertEquals(1, client.get(ApiRoutes.SOCIAL_COUNTS) { bearerAuth(bob.token) }.body<SocialCountsResponse>().unreadMessages)

        client.post(ApiRoutes.conversationRead(alice.id)) { bearerAuth(bob.token) }
        val history: MessagesResponse = client.get(ApiRoutes.conversationMessages(alice.id)) { bearerAuth(bob.token) }.body()
        assertEquals(listOf("hello Bob"), history.items.map { it.body })
        assertTrue(history.items.single().read)
        assertEquals(0, client.get(ApiRoutes.SOCIAL_COUNTS) { bearerAuth(bob.token) }.body<SocialCountsResponse>().unreadMessages)

        // Removing the friend ends the chat.
        assertEquals(HttpStatusCode.NoContent, client.delete(ApiRoutes.friend(alice.id)) { bearerAuth(bob.token) }.status)
        assertEquals(
            HttpStatusCode.Forbidden,
            client.get(ApiRoutes.conversationMessages(alice.id)) { bearerAuth(bob.token) }.status,
        )
    }

    @Test
    fun `the WebSocket pushes counts, then a friend request as it happens`() = testApplication {
        val client = client()
        val alice = client.user("Alice")
        val bob = client.user("Bob")
        val ws = createClient { install(WebSockets) }

        ws.webSocket("${ApiRoutes.SOCIAL_WS}?token=${alice.token}") {
            suspend fun next(): SocialEvent =
                MusicRoomJson.decodeFromString(SocialEvent.serializer(), (incoming.receive() as Frame.Text).readText())

            assertTrue(next() is SocialEvent.Counts)
            client.post(ApiRoutes.friend(alice.id)) { bearerAuth(bob.token) }
            val events = List(3) { withTimeout(5_000) { next() } }
            val notification = events.filterIsInstance<SocialEvent.Notification>().single().notification
            assertEquals(NotificationType.FRIEND_REQUEST, notification.type)
            assertEquals(bob.id, notification.actor?.id)
            assertEquals(1, events.filterIsInstance<SocialEvent.Counts>().last().counts.pendingRequests)
        }
    }

    @Test
    fun `the WebSocket refuses a missing token`() = testApplication {
        client()
        val ws = createClient { install(WebSockets) }
        ws.webSocket(ApiRoutes.SOCIAL_WS) {
            val reason = closeReason.await()
            assertEquals(CloseReason.Codes.VIOLATED_POLICY.code, reason?.code)
        }
    }

    @Test
    fun `a declined request leaves no notification`() = testApplication {
        val client = client()
        val alice = client.user("Alice")
        val bob = client.user("Bob")

        client.post(ApiRoutes.friend(bob.id)) { bearerAuth(alice.token) }
        client.delete(ApiRoutes.friend(alice.id)) { bearerAuth(bob.token) }

        val notifications: NotificationsResponse = client.get(ApiRoutes.NOTIFICATIONS) { bearerAuth(bob.token) }.body()
        assertTrue(notifications.items.isEmpty())
        val friends: FriendsResponse = client.get(ApiRoutes.FRIENDS) { bearerAuth(alice.token) }.body()
        assertTrue(friends.sent.isEmpty())
    }
}
