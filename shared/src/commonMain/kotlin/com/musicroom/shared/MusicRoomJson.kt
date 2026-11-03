package com.musicroom.shared

import kotlinx.serialization.json.Json

val MusicRoomJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}
