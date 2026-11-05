package com.musicroom.server.modules.spotify.client.model

import kotlinx.serialization.Serializable

/** Body of PUT /me/player/play: tracks ([uris]) or a playlist ([contextUri]), and where to start. */
@Serializable
data class SpotifyPlayBody(val uris: List<String>? = null, val contextUri: String? = null, val offset: Offset? = null) {
    /** Where to start: the track's [position] in the list, or the track itself ([uri]). */
    @Serializable
    data class Offset(val position: Int? = null, val uri: String? = null)
}
