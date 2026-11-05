package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/**
 * Everything the event page shows: the song playing, the one chosen to follow, the queue (most voted first, then
 * the oldest), and the people invited. [playbackIssue] (owner only) is an ApiErrors code explaining why the music
 * cannot play on their Spotify (no device open, no Premium...).
 */
@Serializable
data class EventDetailResponse(
    val event: EventResponse,
    val nowPlaying: EventTrackResponse? = null,
    val next: EventTrackResponse? = null,
    val queue: List<EventTrackResponse> = emptyList(),
    val invited: List<PublicUserResponse> = emptyList(),
    val playbackIssue: String? = null,
    /** Where the song playing was when this was sent, and when (epoch ms), to show its progress. */
    val progressMs: Long = 0,
    val sentAt: Long = 0,
)
