package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

@Serializable
enum class EventTrackStatus {
    /** Waiting in the vote queue: its place depends on its votes. */
    QUEUED,

    /** Chosen to play right after the current song: already handed to Spotify, votes no longer move it. */
    NEXT,

    PLAYING,
}
