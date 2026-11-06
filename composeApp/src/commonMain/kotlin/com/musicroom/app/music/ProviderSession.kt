package com.musicroom.app.music

import androidx.compose.runtime.staticCompositionLocalOf

object ProviderSession {
    val onSpotifyConnected = staticCompositionLocalOf<() -> Unit> { {} }
}
