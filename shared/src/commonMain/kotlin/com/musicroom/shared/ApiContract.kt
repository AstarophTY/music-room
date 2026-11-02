package com.musicroom.shared

object ApiRoutes {
    const val HEALTH = "/health"
    const val API_V1 = "/api/v1"
    const val INFO = "$API_V1/info"
    const val USERS = "$API_V1/users"
    const val USERS_ME = "$USERS/me"
}

object ClientHeaders {
    const val PLATFORM = "X-Platform"
    const val DEVICE = "X-Device"
    const val APP_VERSION = "X-App-Version"
}

val MusicRoomJson = kotlinx.serialization.json.Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}
