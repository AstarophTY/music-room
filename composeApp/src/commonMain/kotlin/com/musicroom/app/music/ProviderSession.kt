package com.musicroom.app.music

import androidx.compose.runtime.staticCompositionLocalOf

object ProviderSession {
    val connections = staticCompositionLocalOf<List<MusicProviderConnection>> { emptyList() }
    val onSpotifyConnected = staticCompositionLocalOf<() -> Unit> { {} }
}
