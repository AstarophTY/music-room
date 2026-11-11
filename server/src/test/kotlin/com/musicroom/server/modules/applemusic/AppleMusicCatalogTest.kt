package com.musicroom.server.modules.applemusic

import com.musicroom.server.config.AppleMusicConfig
import com.musicroom.shared.dto.TrackSource
import com.musicroom.server.modules.spotify.client.model.SpotifyExternalIds
import com.musicroom.server.modules.spotify.client.model.SpotifyTrack
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class AppleMusicCatalogTest {
    @Test
    fun `spotify metadata retains the recording code`() {
        val track = SpotifyTrack(id = "spotify-id", externalIds = SpotifyExternalIds(isrc = "USUM71703861"))
        assertEquals("USUM71703861", track.toResponse()?.isrc)
    }
    @Test
    fun `charts return songs with their provider identity`() {
        val response = Json.parseToJsonElement(
            """{"results":{"songs":[{"data":[{"id":"12345","attributes":{"name":"Song","artistName":"Artist","albumName":"Album"}}]}]}}""",
        ).jsonObject
        val page = AppleMusicCatalog(AppleMusicTokenService(AppleMusicConfig())).chartTracks(response, "fr")
        assertEquals(1, page.total)
        assertEquals("apple:fr:12345", page.items.single().id)
        assertEquals(TrackSource.APPLE_MUSIC, page.items.single().source)
    }

    @Test
    fun `album artists point at the artists, not at the album`() {
        val item = Json.parseToJsonElement(
            """{"id":"1000","attributes":{"name":"Album","releaseDate":"2001-05-14","trackCount":12},
               "relationships":{"artists":{"data":[{"id":"777","attributes":{"name":"Artist"}}]}}}""",
        ).jsonObject
        val album = AppleMusicCatalog(AppleMusicTokenService(AppleMusicConfig())).album(item, "fr")
        assertEquals("apple:fr:1000", album.id)
        assertEquals("2001", album.releaseYear)
        assertEquals(listOf("apple:fr:777"), album.artists.map { it.id })
        assertEquals(listOf("777"), album.artists.map { it.uri })
        assertEquals(listOf("Artist"), album.artists.map { it.name })
    }

    @Test
    fun `an album without its artists relationship lists no artist to open`() {
        val item = Json.parseToJsonElement("""{"id":"1000","attributes":{"name":"Album","artistName":"Artist"}}""").jsonObject
        assertEquals(emptyList(), AppleMusicCatalog(AppleMusicTokenService(AppleMusicConfig())).album(item, "fr").artists)
    }

    @Test
    fun `catalog songs retain provider and storefront identity`() {
        val item = Json.parseToJsonElement(
            """{"id":"12345","attributes":{"name":"Song","artistName":"Artist","albumName":"Album","isrc":"USUM71703861","durationInMillis":42000,"contentRating":"explicit","artwork":{"url":"https://example.com/{w}x{h}.jpg"}}}""",
        ).jsonObject
        val track = AppleMusicCatalog(AppleMusicTokenService(AppleMusicConfig())).song(item, "fr")
        assertEquals("apple:fr:12345", track.id)
        assertEquals("12345", track.uri)
        assertEquals(TrackSource.APPLE_MUSIC, track.source)
        assertEquals("USUM71703861", track.isrc)
        assertEquals("https://example.com/300x300.jpg", track.imageUrl)
        assertEquals(42000L, track.durationMs)
        assertEquals(true, track.explicit)
    }
}
