package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/**
 * Plays [uris] (Spotify track URIs), or the playlist [contextUri], starting at [index].
 * Neither: resumes what was playing.
 */
@Serializable
data class PlayRequest(
    val uris: List<String> = emptyList(),
    val index: Int = 0,
    val contextUri: String? = null,
)
