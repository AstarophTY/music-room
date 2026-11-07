package com.musicroom.app.player

import com.musicroom.app.music.AppleMusicPlayback
import com.musicroom.shared.ClientInfo
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.TokenStore
import com.musicroom.shared.dto.AlbumResponse
import com.musicroom.shared.dto.DevicesResponse
import com.musicroom.shared.dto.PlaybackStateResponse
import com.musicroom.shared.dto.QueueResponse
import com.musicroom.shared.dto.TrackResponse
import com.musicroom.shared.dto.TrackSource
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerControllerAppleMusicTest {
    @Test
    fun localEventPlaybackStopsBeforeSpotifyTakesOver() = runTest {
        val fixture = fixture(this)
        try {
            fixture.controller.play(listOf(track("apple-1", TrackSource.APPLE_MUSIC)), 0)
            runCurrent()
            fixture.controller.stopEventLocalPlayback()
            assertFalse(fixture.controller.playingAppleMusic)
            assertEquals(1, fixture.apple.stopCalls)
            assertNull(fixture.controller.track)
        } finally {
            fixture.controller.release()
            fixture.api.close()
        }
    }
    @Test
    fun spotifyTrackInMixedListPlaysWhenApplePlaybackIsUnavailable() = runTest {
        val fixture = fixture(this)
        try {
            fixture.apple.available = false
            val spotifyTrack = track("spotify-1", TrackSource.SPOTIFY)
            val appleTrack = track("apple-1", TrackSource.APPLE_MUSIC)

            fixture.controller.play(listOf(spotifyTrack, appleTrack), 0)
            runCurrent()

            assertEquals(spotifyTrack.id, fixture.controller.track?.id)
            assertTrue(fixture.controller.isPlaying)
            assertTrue(fixture.apple.requests.isEmpty())
        } finally {
            fixture.controller.release()
            fixture.api.close()
        }
    }

    @Test
    fun appleTrackNeedsAConnectionBeforePlaying() = runTest {
        val fixture = fixture(this)
        try {
            fixture.apple.connected = false

            fixture.controller.play(listOf(track("apple-1", TrackSource.APPLE_MUSIC)), 0)
            runCurrent()

            assertFalse(fixture.controller.playingAppleMusic)
            assertTrue(fixture.apple.requests.isEmpty())
            assertNotNull(fixture.controller.error)
        } finally {
            fixture.controller.release()
            fixture.api.close()
        }
    }

    @Test
    fun spotifyContextStopsApplePlayback() = runTest {
        val fixture = fixture(this)
        try {
            val appleTrack = track("apple-1", TrackSource.APPLE_MUSIC)
            val spotifyTrack = track("spotify-1", TrackSource.SPOTIFY)
            fixture.controller.play(listOf(appleTrack), 0)
            runCurrent()

            fixture.controller.playAlbum(AlbumResponse(id = "album", name = "Album", uri = "spotify:album:album"), listOf(spotifyTrack), 0)
            runCurrent()

            assertFalse(fixture.controller.playingAppleMusic)
            assertEquals(spotifyTrack.id, fixture.controller.track?.id)
            assertEquals(1, fixture.apple.stopCalls)
        } finally {
            fixture.controller.release()
            fixture.api.close()
        }
    }

    @Test
    fun staleAppleEndCallbackDoesNotStopReplacementTrack() = runTest {
        val fixture = fixture(this)
        try {
            val first = track("apple-1", TrackSource.APPLE_MUSIC)
            val replacement = track("apple-2", TrackSource.APPLE_MUSIC)
            fixture.controller.play(listOf(first), 0)
            runCurrent()
            val staleEnd = fixture.apple.requests.single().onEnded

            fixture.controller.play(listOf(replacement), 0)
            runCurrent()
            staleEnd()
            runCurrent()

            assertEquals(replacement.id, fixture.controller.track?.id)
            assertTrue(fixture.controller.isPlaying)
            assertEquals(2, fixture.apple.requests.size)
        } finally {
            fixture.controller.release()
            fixture.api.close()
        }
    }

    @Test
    fun queuedAppleTrackPlaysLocallyInsteadOfBeingSentToSpotify() = runTest {
        val fixture = fixture(this)
        try {
            val spotifyTrack = track("spotify-1", TrackSource.SPOTIFY)
            val appleTrack = track("apple-1", TrackSource.APPLE_MUSIC)
            fixture.controller.play(listOf(spotifyTrack), 0)
            runCurrent()
            fixture.controller.addToQueue(listOf(appleTrack))

            fixture.controller.next()
            runCurrent()

            assertTrue(fixture.controller.playingAppleMusic)
            assertEquals(appleTrack.id, fixture.controller.track?.id)
            assertEquals(listOf(appleTrack.uri), fixture.apple.requests.map { it.songId })
            assertTrue(fixture.spotify.queuedUris.isEmpty())
        } finally {
            fixture.controller.release()
            fixture.api.close()
        }
    }

    @Test
    fun spotifyPollingDoesNotReplaceCurrentAppleTrack() = runTest {
        val fixture = fixture(this)
        try {
            val appleTrack = track("apple-1", TrackSource.APPLE_MUSIC)
            fixture.spotify.state = PlaybackStateResponse(active = true, isPlaying = true, track = track("spotify-1", TrackSource.SPOTIFY))
            fixture.controller.play(listOf(appleTrack), 0)
            runCurrent()

            val sync = backgroundScope.launch { fixture.controller.sync() }
            runCurrent()

            assertEquals(appleTrack.id, fixture.controller.track?.id)
            sync.cancel()
        } finally {
            fixture.controller.release()
            fixture.api.close()
        }
    }

    private fun fixture(scope: CoroutineScope): Fixture {
        val api = MusicRoomApi(
            baseUrl = "http://localhost",
            clientInfo = ClientInfo(platform = "test", device = "test", appVersion = "test"),
            engine = TestMockEngineFactory,
            tokenStore = object : TokenStore {
                override var refreshToken: String? = null
            },
        )
        val apple = FakeAppleMusicPlayback()
        val spotify = FakeSpotifyPlayback()
        return Fixture(api, apple, spotify, PlayerController(api, scope, apple, spotify))
    }

    private fun track(id: String, source: TrackSource) = TrackResponse(
        id = id,
        name = id,
        artists = emptyList(),
        album = "Album",
        durationMs = 180_000,
        uri = "${source.name.lowercase()}:$id",
        source = source,
    )

    private data class Fixture(
        val api: MusicRoomApi,
        val apple: FakeAppleMusicPlayback,
        val spotify: FakeSpotifyPlayback,
        val controller: PlayerController,
    )

    private class FakeAppleMusicPlayback : AppleMusicPlayback {
        override var available = true
        override var connected = true
        val requests = mutableListOf<Request>()
        var stopCalls = 0
        private var position = 0L

        override suspend fun play(songId: String, onEnded: () -> Unit, onError: (Exception) -> Unit) {
            requests += Request(songId, onEnded, onError)
        }

        override fun pause() = Unit
        override fun resume() = Unit
        override fun seek(positionMs: Long) { position = positionMs }
        override fun positionMs() = position
        override fun stop() { stopCalls++ }

        data class Request(val songId: String, val onEnded: () -> Unit, val onError: (Exception) -> Unit)
    }

    private class FakeSpotifyPlayback : SpotifyRemotePlaybackProvider {
        var state = PlaybackStateResponse(active = false)
        val queuedUris = mutableListOf<String>()

        override suspend fun playbackState() = state
        override suspend fun play(uris: List<String>, index: Int, contextUri: String?) = Unit
        override suspend fun pause() = Unit
        override suspend fun skipNext() = Unit
        override suspend fun skipPrevious() = Unit
        override suspend fun seek(positionMs: Long) = Unit
        override suspend fun devices() = DevicesResponse()
        override suspend fun transferPlayback(deviceId: String, play: Boolean) = Unit
        override suspend fun queue() = QueueResponse()
        override suspend fun addToQueue(uris: List<String>) { queuedUris += uris }
    }

    private object TestMockEngineFactory : HttpClientEngineFactory<MockEngineConfig> {
        override fun create(block: MockEngineConfig.() -> Unit): HttpClientEngine =
            MockEngine { error("Unexpected API request") }
    }
}
