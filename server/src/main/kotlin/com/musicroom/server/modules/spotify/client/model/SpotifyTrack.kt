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
    val externalIds: SpotifyExternalIds? = null,
    val uri: String = "",
    /** The track asked for, when Spotify plays another version of it available in the user's country. */
    val linkedFrom: LinkedTrack? = null,
) {
    @Serializable
    data class LinkedTrack(val uri: String = "")

    /**
     * Null for what the app cannot play from the catalog (local files, episodes). The tracks of an album
     * page have no album: [fromAlbum] gives its name and cover.
     */
    fun toResponse(fromAlbum: SpotifyAlbum? = null): TrackResponse? {
        if (id == null || type != "track") return null
        val source = album ?: fromAlbum
        return TrackResponse(
            id = id,
            name = name,
            artists = artists.map { it.name },
            album = source?.name.orEmpty(),
            imageUrl = source?.images?.medium()?.url,
            durationMs = durationMs,
            explicit = explicit,
            spotifyUrl = externalUrls?.spotify,
            uri = uri,
            isrc = externalIds?.isrc,
        )
    }
}
