package com.musicroom.app.search

import kotlin.test.Test
import kotlin.test.assertEquals

class SearchRouteTest {
    @Test
    fun stackRoundTripsNamesAndIdentifiersContainingSeparators() {
        val routes = listOf(
            SearchRoute.Mood(3),
            SearchRoute.Artist("artist|id", "Artist | Name"),
            SearchRoute.Album("album|id", "Album | Name"),
        )
        assertEquals(routes, SearchRoute.decode(SearchRoute.encode(routes)))
    }

    @Test
    fun restoresLegacyStacks() {
        val saved = listOf("mood|3|", "artist|artist-id|Artist | Name", "album|album-id|Album")
        assertEquals(
            listOf(SearchRoute.Mood(3), SearchRoute.Artist("artist-id", "Artist | Name"), SearchRoute.Album("album-id", "Album")),
            SearchRoute.decode(saved),
        )
    }

    @Test
    fun ignoresInvalidSavedEntries() {
        assertEquals(listOf(SearchRoute.Artist("a", "Artist")), SearchRoute.decode(listOf("mood|invalid|", "artist|a|Artist")))
        assertEquals(emptyList(), SearchRoute.decode(emptyList()))
    }
}
