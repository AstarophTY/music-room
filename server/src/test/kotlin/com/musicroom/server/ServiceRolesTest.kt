package com.musicroom.server

import com.musicroom.server.config.AppConfig
import com.musicroom.server.config.DatabaseConfig
import com.musicroom.server.config.JwtConfig
import com.musicroom.server.config.ServiceRole
import com.musicroom.server.database.TestDatabase
import com.musicroom.server.modules.social.PostgresSocialRelay
import com.musicroom.shared.ApiRoutes
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers(disabledWithoutDocker = true)
class ServiceRolesTest {

    private fun config(role: ServiceRole) = AppConfig(
        version = "test",
        database = DatabaseConfig("unused", "unused", "unused"),
        jwt = JwtConfig(secret = "x".repeat(32), issuer = "test", audience = "test", realm = "test"),
        role = role,
    )

    private fun assertRoutes(role: ServiceRole, served: String, notServed: String) = testApplication {
        environment { config = MapApplicationConfig() }
        application { bootstrap(AppModule(config(role), TestDatabase.module, emailSender = { _, _, _ -> })) }
        assertEquals(HttpStatusCode.OK, client.get(ApiRoutes.HEALTH).status)
        assertNotEquals(HttpStatusCode.NotFound, client.get(served).status)
        assertEquals(HttpStatusCode.NotFound, client.get(notServed).status)
    }

    @Test
    fun `identity serves accounts only`() = assertRoutes(ServiceRole.IDENTITY, ApiRoutes.USERS_ME, ApiRoutes.EVENTS)

    @Test
    fun `music serves the catalog only`() = assertRoutes(ServiceRole.MUSIC, ApiRoutes.SPOTIFY_STATUS, ApiRoutes.PLAYLISTS)

    @Test
    fun `library serves playlists only`() = assertRoutes(ServiceRole.LIBRARY, ApiRoutes.PLAYLISTS, ApiRoutes.FRIENDS)

    @Test
    fun `realtime serves events and social only`() = assertRoutes(ServiceRole.REALTIME, ApiRoutes.EVENTS, ApiRoutes.PLAYLISTS)

    @Test
    fun `an unknown role is refused`() {
        assertEquals(ServiceRole.LIBRARY, ServiceRole.parse(" library "))
        assertFailsWith<IllegalArgumentException> { ServiceRole.parse("nope") }
    }

    @Test
    fun `a social event crosses services through PostgreSQL`() = runBlocking {
        val relay = PostgresSocialRelay(TestDatabase.module, TestDatabase.config)
        val received = Channel<Pair<UUID, String>>(Channel.UNLIMITED)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val userId = UUID.randomUUID()
        val text = """{"type":"friends","note":"a|b"}"""
        try {
            relay.listen(scope) { id, event -> received.send(id to event) }
            // LISTEN starts in the background: publish until it is up.
            val publisher = scope.launch {
                while (true) {
                    relay.publish(userId, text)
                    delay(100)
                }
            }
            assertEquals(userId to text, withTimeout(10_000) { received.receive() })
            publisher.cancel()
        } finally {
            scope.cancel()
        }
    }
}
