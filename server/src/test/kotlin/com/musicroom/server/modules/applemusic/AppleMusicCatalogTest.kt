package com.musicroom.server.modules.applemusic

import com.musicroom.server.config.AppleMusicConfig
import com.musicroom.shared.dto.TrackSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class AppleMusicCatalogTest {
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
    fun `catalog songs retain provider and storefront identity`() {
        val item = Json.parseToJsonElement(
            """{"id":"12345","attributes":{"name":"Song","artistName":"Artist","albumName":"Album","durationInMillis":42000,"contentRating":"explicit","artwork":{"url":"https://example.com/{w}x{h}.jpg"}}}""",
        ).jsonObject
        val track = AppleMusicCatalog(AppleMusicTokenService(AppleMusicConfig())).song(item, "fr")
        assertEquals("apple:fr:12345", track.id)
        assertEquals("12345", track.uri)
        assertEquals(TrackSource.APPLE_MUSIC, track.source)
        assertEquals("https://example.com/300x300.jpg", track.imageUrl)
        assertEquals(42000L, track.durationMs)
        assertEquals(true, track.explicit)
    }
}
