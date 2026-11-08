package com.musicroom.app.music

import com.musicroom.shared.dto.SearchResponse
import com.musicroom.shared.dto.TrackResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MusicCatalogSearchTest {
    private class Provider(private val result: SearchResponse? = null, private val failure: Exception? = null) : MusicCatalogSearch.Provider {
        override suspend fun search(query: String, mood: String?): SearchResponse = result ?: throw requireNotNull(failure)
        override suspend fun suggestions(): List<TrackResponse> = search("", null).tracks
    }

    private fun track(id: String) = TrackResponse(id, id, emptyList(), "", durationMs = 1, uri = id)

    @Test
    fun mixesTracksAndPreservesSuccessfulResultsWhenAnotherProviderFails() = runTest {
        val catalog = MusicCatalogSearch(listOf(
            Provider(SearchResponse(tracks = listOf(track("a"), track("b")), nextTrackOffset = 25)),
            Provider(SearchResponse(tracks = listOf(track("c")))),
            Provider(failure = IllegalStateException("unavailable")),
        ))
        val result = catalog.search("test")
        assertEquals(listOf("a", "c", "b"), result.response?.tracks?.map { it.id })
        assertEquals(25, result.response?.nextTrackOffset)
        assertEquals(1, result.failures.size)
    }

    @Test
    fun cancellationIsNotSwallowed() = runTest {
        val catalog = MusicCatalogSearch(listOf(Provider(failure = CancellationException("cancelled"))))
        assertFailsWith<CancellationException> { catalog.search("test") }
    }
}
