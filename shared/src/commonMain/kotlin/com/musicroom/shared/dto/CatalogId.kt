package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/**
 * A provider-owned identifier, independent of its legacy wire and database representation.
 * Apple catalog IDs are scoped to the storefront: availability and metadata can differ by region,
 * so tracks from different storefronts remain distinct even when their recording codes match.
 * Legacy IDs keep their original casing for compatibility with stored references.
 */
@Serializable
data class CatalogId(val source: TrackSource, val externalId: String, val storefront: String? = null) {
    /** Preserves the identifiers already stored by previous versions. */
    fun toLegacy(): String = when (source) {
        TrackSource.SPOTIFY -> externalId
        TrackSource.MUSIC_ROOM -> TrackResponse.HOSTED_PREFIX + externalId
        TrackSource.APPLE_MUSIC -> "apple:$storefront:$externalId"
    }

    companion object {
        fun fromLegacy(value: String): CatalogId? {
            if (value.isBlank()) return null
            if (value.startsWith("apple:")) {
                val parts = value.split(':')
                if (parts.size != 3 || !parts[1].matches(Regex("[A-Za-z]{2}")) || !parts[2].matches(Regex("[0-9]{1,32}"))) return null
                return CatalogId(TrackSource.APPLE_MUSIC, parts[2], parts[1])
            }
            if (value.startsWith(TrackResponse.HOSTED_PREFIX)) {
                val id = value.removePrefix(TrackResponse.HOSTED_PREFIX)
                return id.takeIf { it.isNotBlank() && ':' !in it }?.let { CatalogId(TrackSource.MUSIC_ROOM, it) }
            }
            if (':' in value) return null
            return CatalogId(TrackSource.SPOTIFY, value)
        }
    }
}
