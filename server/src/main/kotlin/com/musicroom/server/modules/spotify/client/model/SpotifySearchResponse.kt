package com.musicroom.server.modules.spotify.client.model

import kotlinx.serialization.Serializable

@Serializable
data class SpotifySearchResponse(val tracks: SpotifyPaging<SpotifyTrack> = SpotifyPaging())
