package com.musicroom.app.music

import com.musicroom.shared.dto.SearchResponse
import com.musicroom.shared.dto.TrackResponse
import com.musicroom.shared.dto.TrackSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MusicProviderRegistryTest {
    private class Provider(
        override val source: TrackSource,
        override val connected: Boolean = true,
        override val searchable: Boolean = true,
        private val playable: Boolean = true,
        override val providesRemotePlayback: Boolean = false,
    ) : MusicProviderConnection {
        override val available = true
        override val displayName = source.name
        override suspend fun canPlay() = playable
        override suspend fun connect() = connected
        override suspend fun disconnect() = Unit
        override suspend fun search(query: String, mood: String?) = SearchResponse(tracks = listOf(track(source)))
        override suspend fun suggestions() = listOf(track(source))
        override suspend fun find(isrc: String): TrackResponse? = null
    }

    companion object {
        private fun track(source: TrackSource) = TrackResponse(source.name, "Track", emptyList(), "", durationMs = 1, uri = "1", source = source)
    }

    @Test
    fun searchMixesConnectedCatalogsEvenWhenPlaybackIsUnavailable() = runTest {
        val spotify = Provider(TrackSource.SPOTIFY, playable = false, providesRemotePlayback = true)
        val apple = Provider(TrackSource.APPLE_MUSIC)
        val registry = MusicProviderRegistry(listOf(spotify, apple))
        assertEquals(listOf(TrackSource.SPOTIFY, TrackSource.APPLE_MUSIC), registry.catalogSearch().search("song").response?.tracks?.map { it.source })
        val availability = registry.playbackAvailability()
        assertTrue(availability.canListen)
        assertFalse(availability.hostControlsPlayback(track(TrackSource.SPOTIFY)))
        assertEquals(track(TrackSource.APPLE_MUSIC), availability.resolve(track(TrackSource.APPLE_MUSIC)))
    }

    @Test
    fun connectedProviderWithoutCatalogCanStillPlayItsOwnTrack() = runTest {
        val apple = Provider(TrackSource.APPLE_MUSIC, searchable = false)
        val registry = MusicProviderRegistry(listOf(apple))
        assertNull(registry.catalogSearch().search("song").response)
        assertEquals(track(TrackSource.APPLE_MUSIC), registry.playbackAvailability().resolve(track(TrackSource.APPLE_MUSIC)))
        assertFalse(MusicProviderRegistry(listOf(Provider(TrackSource.APPLE_MUSIC, connected = false))).playbackAvailability().canListen)
    }
}
