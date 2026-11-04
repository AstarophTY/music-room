package com.musicroom.server.modules.spotify.client.model

import com.musicroom.shared.dto.PlaylistSummaryResponse
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyPlaylist(
    val id: String,
    val name: String = "",
    val images: List<SpotifyImage>? = null,
    val uri: String = "",
    /** Track count: [items] since the February 2026 API changes, [tracks] before. */
    val items: TracksRef? = null,
    val tracks: TracksRef? = null,
    val owner: Owner? = null,
    val externalUrls: SpotifyExternalUrls? = null,
) {
    @Serializable
    data class TracksRef(val total: Int = 0)

    @Serializable
    data class Owner(val id: String? = null, val displayName: String? = null)

    fun toResponse() = PlaylistSummaryResponse(
        id = id,
        name = name,
        imageUrl = images?.firstOrNull()?.url,
        trackCount = (items ?: tracks)?.total,
        uri = uri.ifEmpty { null },
        owner = owner?.displayName,
        spotifyUrl = externalUrls?.spotify,
    )
}
