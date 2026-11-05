package com.musicroom.server.modules.spotify.client.model

import com.musicroom.shared.dto.ArtistResponse
import kotlinx.serialization.Serializable

/** Artist object of the Spotify Web API; inside a track or an album it has no [images]. */
@Serializable
data class SpotifyArtist(
    val id: String? = null,
    val name: String,
    val images: List<SpotifyImage> = emptyList(),
    val uri: String = "",
) {
    /** Null without an id (local files). */
    fun toResponse(): ArtistResponse? {
        if (id == null) return null
        return ArtistResponse(id = id, name = name, imageUrl = images.medium()?.url, uri = uri.ifEmpty { "spotify:artist:$id" })
    }
}
