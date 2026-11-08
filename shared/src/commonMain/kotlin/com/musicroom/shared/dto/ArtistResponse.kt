package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** An artist of the Spotify catalog. [uri] is the Spotify URI. */
@Serializable
data class ArtistResponse(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val uri: String,
) {
    val catalogId: CatalogId? get() = CatalogId.fromLegacy(id)
}
