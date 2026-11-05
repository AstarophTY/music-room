package com.musicroom.server.modules.spotify.client.model

import com.musicroom.shared.dto.TrackResponse
import kotlinx.serialization.Serializable

/** Track object of the Spotify Web API. Local files have no id; podcast episodes have type "episode". */
@Serializable
data class SpotifyTrack(
    val id: String? = null,
    val name: String = "",
    val type: String = "track",
    val artists: List<SpotifyArtist> = emptyList(),
    val album: SpotifyAlbum? = null,
    val durationMs: Long = 0,
    val explicit: Boolean = false,
    val externalUrls: SpotifyExternalUrls? = null,
    val uri: String = "",
) {
    /** Null for what the app cannot play from the catalog (local files, episodes). */
    fun toResponse(): TrackResponse? {
        if (id == null || type != "track") return null
        return TrackResponse(
            id = id,
            name = name,
            artists = artists.map { it.name },
            album = album?.name.orEmpty(),
            // Spotify lists images from the largest; the medium one (300 px) is enough for lists.
            imageUrl = album?.images?.let { it.getOrNull(1) ?: it.firstOrNull() }?.url,
            durationMs = durationMs,
            explicit = explicit,
            spotifyUrl = externalUrls?.spotify,
            uri = uri,
        )
    }
}
