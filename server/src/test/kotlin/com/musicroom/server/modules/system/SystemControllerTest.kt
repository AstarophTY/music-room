package com.musicroom.server.modules.system

import com.musicroom.server.common.register
import com.musicroom.server.common.serialization.configureSerialization
import com.musicroom.shared.ApiRoutes
import com.musicroom.shared.MusicRoomJson
import com.musicroom.shared.dto.HealthResponse
import com.musicroom.shared.dto.InfoResponse
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class SystemControllerTest {

    private fun ApplicationTestBuilder.setup(dbUp: Boolean) {
        environment { config = MapApplicationConfig() }
        application {
            configureSerialization()
            routing { register(SystemController(SystemService(version = "test", isDatabaseUp = { dbUp }))) }
        }
    }

    private fun ApplicationTestBuilder.jsonClient() = createClient {
        install(ContentNegotiation) { json(MusicRoomJson) }
    }

    @Test
    fun `health returns 200 when database is up`() = testApplication {
        setup(dbUp = true)
        val response = jsonClient().get(ApiRoutes.HEALTH)
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(HealthResponse("ok", "up"), response.body())
    }

    @Test
    fun `health returns 503 when database is down`() = testApplication {
        setup(dbUp = false)
        assertEquals(HttpStatusCode.ServiceUnavailable, jsonClient().get(ApiRoutes.HEALTH).status)
    }

    @Test
    fun `info returns version`() = testApplication {
        setup(dbUp = true)
        assertEquals(InfoResponse("music-room", "test"), jsonClient().get(ApiRoutes.INFO).body())
    }
}
