package com.musicroom.app.music

import com.musicroom.shared.dto.SearchResponse
import com.musicroom.shared.dto.TrackPageResponse
import com.musicroom.shared.dto.TrackResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MusicCatalogSearchTest {
    private class Provider(
        private val result: SearchResponse? = null,
        private val failure: Exception? = null,
        private val next: suspend (Int) -> TrackPageResponse? = { null },
    ) : MusicCatalogSearch.Provider {
        override suspend fun search(query: String, mood: String?): SearchResponse = result ?: throw requireNotNull(failure)
        override suspend fun suggestions(): List<TrackResponse> = search("", null).tracks
        override suspend fun nextTracks(query: String, offset: Int) = next(offset)
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

    @Test
    fun nextPageMixesProvidersAndKeepsTheCursorOfAFailedOne() = runTest {
        val paging = Provider(
            result = SearchResponse(tracks = listOf(track("a")), nextTrackOffset = 25),
            next = { TrackPageResponse(listOf(track("c")), total = 2, nextOffset = 50) },
        )
        val failing = Provider(
            result = SearchResponse(tracks = listOf(track("b")), nextTrackOffset = 10),
            next = { throw IllegalStateException("unavailable") },
        )
        val catalog = MusicCatalogSearch(listOf(paging, failing))
        val first = catalog.search("test")
        assertEquals(listOf(25, 10), first.cursors.map { it.offset })

        val next = catalog.nextTracks("test", first.cursors)

        assertEquals(listOf("c"), next.response?.tracks?.map { it.id })
        assertEquals(1, next.failures.size)
        // The failed provider keeps its cursor so the page can be retried.
        assertEquals(listOf(50, 10), next.cursors.map { it.offset })
    }
}
