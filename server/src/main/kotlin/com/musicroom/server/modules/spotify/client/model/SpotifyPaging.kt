package com.musicroom.server.modules.spotify.client.model

import kotlinx.serialization.Serializable

/** Paged list of the Spotify Web API. */
@Serializable
data class SpotifyPaging<T>(
    val items: List<T> = emptyList(),
    val total: Int = 0,
    val offset: Int = 0,
    val limit: Int = 0,
    val next: String? = null,
) {
    val nextOffset: Int? get() = if (next != null) offset + items.size else null
}
