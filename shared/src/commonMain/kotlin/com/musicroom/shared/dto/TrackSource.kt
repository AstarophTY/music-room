package com.musicroom.shared.dto

/** The provider that owns a music catalog item or hosted track. */
enum class TrackSource {
    SPOTIFY, MUSIC_ROOM, APPLE_MUSIC;

    companion object {
        /** Existing catalog identifiers encode Apple Music and hosted tracks with prefixes. */
        fun fromId(id: String): TrackSource = when {
            id.startsWith("apple:") -> APPLE_MUSIC
            id.startsWith(TrackResponse.HOSTED_PREFIX) -> MUSIC_ROOM
            else -> SPOTIFY
        }
    }
}
