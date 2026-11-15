package com.musicroom.app.music

import com.musicroom.app.events.EventTrackResolver
import com.musicroom.shared.dto.TrackResponse
import com.musicroom.shared.dto.TrackSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MusicPlaybackAvailabilityTest {
    private class Provider(override val source: TrackSource) : EventTrackResolver.Provider {
        override suspend fun find(isrc: String): TrackResponse? = null
    }

    @Test
    fun listeningAndHostControlAreSeparateCapabilities() = runTest {
        val source = TrackSource.APPLE_MUSIC
        val track = TrackResponse("apple:us:1", "Track", emptyList(), "", durationMs = 1, uri = "1", source = source)
        val available = MusicPlaybackAvailability(EventTrackResolver(listOf(Provider(source))), setOf(source), emptySet())
        assertTrue(available.canListen)
        assertFalse(available.hostControlsPlayback(track))
        assertEquals(track, available.resolve(track))
        val remote = MusicPlaybackAvailability(EventTrackResolver(listOf(Provider(source))), setOf(source), setOf(source))
        assertTrue(remote.hostControlsPlayback(track))
    }
}
