package com.musicroom.server.modules.spotify.client.model

/** Spotify lists images from the largest; the medium one (about 300 px) is enough for lists. */
fun List<SpotifyImage>.medium(): SpotifyImage? = getOrNull(1) ?: firstOrNull()
