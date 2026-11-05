package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/**
 * A track, as the app needs it: from the Spotify catalog ([uri] is the Spotify URI used for playback), or uploaded
 * to Music Room by a Premium user ([source] MUSIC_ROOM, played by the app itself; its [id] starts with [HOSTED_PREFIX]).
 */
@Serializable
data class TrackResponse(
    val id: String,
    val name: String,
    val artists: List<String>,
    val album: String,
    val imageUrl: String? = null,
    val durationMs: Long,
    val explicit: Boolean = false,
    val spotifyUrl: String? = null,
    val uri: String,
    val source: TrackSource = TrackSource.SPOTIFY,
) {
    /** Uploaded to Music Room: the app plays it, Spotify cannot. */
    val hosted: Boolean get() = source == TrackSource.MUSIC_ROOM

    companion object {
        /** Start of the [id] of the songs uploaded to Music Room. */
        const val HOSTED_PREFIX = "mr-"
    }
}
