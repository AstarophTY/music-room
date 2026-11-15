package com.musicroom.app.playlists

import com.musicroom.app.common.UiText
import com.musicroom.app.music.MusicPlaylist
import com.musicroom.app.music.MusicProviderConnection

/** One provider's playlist shelf: loading while [playlists] and [error] are both null. */
data class ProviderPlaylists(
    val provider: MusicProviderConnection,
    val playlists: List<MusicPlaylist>? = null,
    val error: UiText? = null,
)
