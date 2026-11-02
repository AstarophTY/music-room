package com.musicroom.shared

import com.musicroom.shared.dto.HealthResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class SerializationTest {
    @Test
    fun healthResponseRoundTrip() {
        val original = HealthResponse(status = "ok", database = "up")
        val json = MusicRoomJson.encodeToString(HealthResponse.serializer(), original)
        assertEquals(original, MusicRoomJson.decodeFromString(HealthResponse.serializer(), json))
    }

    @Test
    fun unknownKeysAreIgnored() {
        val json = """{"status":"ok","database":"up","extra":42}"""
        assertEquals("ok", MusicRoomJson.decodeFromString(HealthResponse.serializer(), json).status)
    }
}
