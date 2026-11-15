package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/**
 * An album (or single, EP) of the Spotify catalog. [releaseYear] is null when Spotify does not give it;
 * [uri] plays it as a whole.
 */
@Serializable
data class AlbumResponse(
    val id: String,
    val name: String,
    val artists: List<ArtistResponse> = emptyList(),
    val imageUrl: String? = null,
    val releaseYear: String? = null,
    val trackCount: Int? = null,
    val type: AlbumType = AlbumType.ALBUM,
    val uri: String,
) {
    val catalogId: CatalogId? get() = CatalogId.fromLegacy(id)
}
