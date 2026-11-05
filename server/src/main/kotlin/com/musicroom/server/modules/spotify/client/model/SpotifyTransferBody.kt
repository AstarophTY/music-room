package com.musicroom.server.modules.spotify.client.model

import kotlinx.serialization.Serializable

/** Body of PUT /me/player: the device to move playback to (Spotify takes only one) and whether to play. */
@Serializable
data class SpotifyTransferBody(val deviceIds: List<String>, val play: Boolean)
