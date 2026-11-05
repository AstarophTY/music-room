package com.musicroom.server.modules.spotify.client.model

import kotlinx.serialization.Serializable

/** Answer of GET /search: one page for each type asked. */
@Serializable
data class SpotifySearchResponse(
    val tracks: SpotifyPaging<SpotifyTrack> = SpotifyPaging(),
    val artists: SpotifyPaging<SpotifyArtist> = SpotifyPaging(),
    val albums: SpotifyPaging<SpotifyAlbum> = SpotifyPaging(),
)
