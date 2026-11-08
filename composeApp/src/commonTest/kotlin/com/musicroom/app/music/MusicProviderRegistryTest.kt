package com.musicroom.app.music

import com.musicroom.shared.ClientInfo
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.TokenStore
import com.musicroom.shared.dto.TrackResponse
import com.musicroom.shared.dto.TrackSource
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MusicProviderRegistryTest {
    private object Engine : HttpClientEngineFactory<MockEngineConfig> {
        override fun create(block: MockEngineConfig.() -> Unit): HttpClientEngine = MockEngine { error("Unexpected request") }
    }

    @Test
    fun appleConnectionWithoutStorefrontCanPlayItsOwnTrackButCannotSearch() = runTest {
        val api = MusicRoomApi("http://localhost", ClientInfo("test", "test", "test"), Engine, object : TokenStore {
            override var refreshToken: String? = null
        })
        try {
            val registry = MusicProviderRegistry.connected(api, spotify = false, appleConnected = true, appleStorefront = null)
            val track = TrackResponse("apple:us:1", "Track", emptyList(), "", durationMs = 1, uri = "1", source = TrackSource.APPLE_MUSIC)
            assertEquals(track, registry.eventTrackResolver().resolve(track))
            assertNull(registry.catalogSearch().search("song").response)
            assertNull(MusicProviderRegistry.connected(api, false, false, null).eventTrackResolver().resolve(track))
        } finally {
            api.close()
        }
    }
}
