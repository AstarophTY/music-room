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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppleMusicProviderConnectionTest {
    /**
     * Apple Music plays on the device only: the server has no Apple playback, and the home screen's
     * recommendations come from Spotify. Claiming either leaves an Apple-only listener in silence or in error.
     */
    @Test
    fun appleMusicPlaysLocallyAndRecommendsNothing() = runTest {
        val api = api()
        try {
            val apple = AppleMusicProviderConnection(api, FakeConnection(), FakePlayback())
            assertFalse(apple.providesRemotePlayback)
            assertFalse(apple.providesRecommendations)

            val availability = MusicProviderRegistry(listOf(apple)).playbackAvailability()
            assertTrue(availability.canListen)
            // The host's own phone plays the song; the server is not expected to drive it.
            assertFalse(availability.hostControlsPlayback(appleTrack))
        } finally {
            api.close()
        }
    }

    @Test
    fun searchNeedsAStorefrontEvenOnceConnected() = runTest {
        val api = api()
        try {
            assertTrue(AppleMusicProviderConnection(api, FakeConnection(), FakePlayback()).searchable)
            assertFalse(AppleMusicProviderConnection(api, FakeConnection(storefront = null), FakePlayback()).searchable)
        } finally {
            api.close()
        }
    }

    private fun api() = MusicRoomApi(
        baseUrl = "http://localhost",
        clientInfo = ClientInfo(platform = "test", device = "test", appVersion = "test"),
        engine = TestMockEngineFactory,
        tokenStore = object : TokenStore {
            override var refreshToken: String? = null
        },
    )

    private val appleTrack = TrackResponse(
        id = "apple:us:1",
        name = "Track",
        artists = emptyList(),
        album = "Album",
        durationMs = 180_000,
        uri = "1",
        source = TrackSource.APPLE_MUSIC,
    )

    private class FakeConnection(override val storefront: String? = "us") : AppleMusicConnection {
        override val available = true
        override val connected = true
        override suspend fun connect(developerToken: String) = true
        override fun disconnect() = Unit
    }

    private class FakePlayback : AppleMusicPlayback {
        override val available = true
        override val connected = true
        override suspend fun play(songId: String, onEnded: () -> Unit, onError: (Exception) -> Unit) = Unit
        override fun pause() = Unit
        override fun resume() = Unit
        override fun seek(positionMs: Long) = Unit
        override fun positionMs() = 0L
        override fun stop() = Unit
    }

    private object TestMockEngineFactory : HttpClientEngineFactory<MockEngineConfig> {
        override fun create(block: MockEngineConfig.() -> Unit): HttpClientEngine =
            MockEngine { error("Unexpected API request") }
    }
}
