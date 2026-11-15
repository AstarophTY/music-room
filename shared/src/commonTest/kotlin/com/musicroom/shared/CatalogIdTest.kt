package com.musicroom.shared

import com.musicroom.shared.dto.AddPlaylistTrackRequest
import com.musicroom.shared.dto.CatalogId
import com.musicroom.shared.dto.ProposeTrackRequest
import com.musicroom.shared.dto.TrackSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CatalogIdTest {
    @Test
    fun legacyIdentifiersRoundTripWithoutChangingStoredValues() {
        for (value in listOf("spotify123", "mr-123", "apple:us:12345")) {
            assertEquals(value, CatalogId.fromLegacy(value)?.toLegacy())
        }
    }

    @Test
    fun appleStorefrontCasingDoesNotCreateASecondIdentity() {
        assertEquals(CatalogId(TrackSource.APPLE_MUSIC, "12345", "us"), CatalogId.fromLegacy("apple:US:12345"))
        assertEquals("apple:us:12345", CatalogId.fromLegacy("apple:US:12345")?.toLegacy())
        assertEquals(CatalogId.fromLegacy("apple:us:12345"), CatalogId.fromLegacy("apple:US:12345"))
    }

    @Test
    fun rejectsMalformedOrUnknownProviderIdentifiers() {
        for (value in listOf("", "apple:us:", "apple:usa:1", "apple:us:x", "apple:us:1:extra", "unknown:123", "mr-")) {
            assertNull(CatalogId.fromLegacy(value), value)
        }
    }

    @Test
    fun legacyRequestsDecodeWithoutTypedIdentity() {
        val playlist = MusicRoomJson.decodeFromString(AddPlaylistTrackRequest.serializer(), """{"trackId":"spotify123"}""")
        val event = MusicRoomJson.decodeFromString(ProposeTrackRequest.serializer(), """{"trackId":"apple:us:123"}""")
        assertNull(playlist.catalogId)
        assertNull(event.catalogId)
        val identity = CatalogId.fromLegacy(event.trackId)
        val encoded = MusicRoomJson.encodeToString(ProposeTrackRequest.serializer(), event.copy(catalogId = identity))
        assertEquals(identity, MusicRoomJson.decodeFromString(ProposeTrackRequest.serializer(), encoded).catalogId)
    }
}
