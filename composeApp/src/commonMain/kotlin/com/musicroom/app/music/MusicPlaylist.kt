package com.musicroom.app.music

import com.musicroom.shared.dto.TrackSource

/** A playlist from a connected provider's account, shown and played through the common provider contract. */
data class MusicPlaylist(
    val source: TrackSource,
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val trackCount: Int? = null,
    val owner: String? = null,
    /** Opens the playlist in the provider's own app or site. */
    val externalUrl: String? = null,
    /** Opaque provider context that plays the playlist as a whole, when it has one. */
    val contextUri: String? = null,
) {
    /** Identifies the playlist across providers, e.g. as a navigation key. */
    val key: String get() = "${source.name}:$id"
}
