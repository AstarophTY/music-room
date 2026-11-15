package com.musicroom.app.events

import com.musicroom.shared.dto.TrackResponse
import com.musicroom.shared.dto.TrackSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EventTrackResolverTest {
    private class Provider(override val source: TrackSource, private val result: TrackResponse? = null) : EventTrackResolver.Provider {
        var calls = 0
        override suspend fun find(isrc: String): TrackResponse? { calls++; return result }
    }

    private fun track(source: TrackSource, isrc: String? = "USABC1234567") =
        TrackResponse("id", "title", emptyList(), "album", durationMs = 1000, uri = "id", source = source, isrc = isrc)

    @Test
    fun keepsOriginalWhenItsProviderIsConnected() = runTest {
        val provider = Provider(TrackSource.APPLE_MUSIC)
        val original = track(TrackSource.APPLE_MUSIC)
        assertEquals(original, EventTrackResolver(listOf(provider)).resolve(original))
        assertEquals(0, provider.calls)
    }

    @Test
    fun usesMatchingRecordingFromAnotherProvider() = runTest {
        val match = track(TrackSource.APPLE_MUSIC, "usabc1234567")
        val provider = Provider(TrackSource.APPLE_MUSIC, match)
        assertEquals(match, EventTrackResolver(listOf(provider)).resolve(track(TrackSource.SPOTIFY)))
        assertEquals(1, provider.calls)
    }

    @Test
    fun rejectsMissingOrMismatchedRecordingCodes() = runTest {
        val provider = Provider(TrackSource.APPLE_MUSIC, track(TrackSource.APPLE_MUSIC, "USXYZ1234567"))
        val resolver = EventTrackResolver(listOf(provider))
        assertNull(resolver.resolve(track(TrackSource.SPOTIFY, null)))
        assertNull(resolver.resolve(track(TrackSource.SPOTIFY)))
    }
}
