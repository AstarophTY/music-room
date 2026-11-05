package com.musicroom.server.modules.spotify.client.model

import com.musicroom.shared.dto.AlbumResponse
import com.musicroom.shared.dto.AlbumType
import kotlinx.serialization.Serializable

/**
 * Album object of the Spotify Web API. Inside a track only [name] and [images] matter; GET /albums/{id}
 * also lists its [tracks] (without their album).
 */
@Serializable
data class SpotifyAlbum(
    val id: String? = null,
    val name: String = "",
    val images: List<SpotifyImage> = emptyList(),
    val artists: List<SpotifyArtist> = emptyList(),
    val albumType: String? = null,
    val releaseDate: String? = null,
    val totalTracks: Int? = null,
    val uri: String = "",
    val tracks: SpotifyPaging<SpotifyTrack>? = null,
) {
    /** Null without an id. */
    fun toResponse(): AlbumResponse? {
        if (id == null) return null
        return AlbumResponse(
            id = id,
            name = name,
            artists = artists.mapNotNull { it.toResponse() },
            imageUrl = images.medium()?.url,
            // "2021-05-14", "2021-05" or "2021".
            releaseYear = releaseDate?.take(4)?.takeIf { it.length == 4 },
            trackCount = totalTracks,
            type = when (albumType?.lowercase()) {
                "single" -> AlbumType.SINGLE
                "compilation" -> AlbumType.COMPILATION
                else -> AlbumType.ALBUM
            },
            uri = uri.ifEmpty { "spotify:album:$id" },
        )
    }
}
