package com.musicroom.app.music

import androidx.compose.runtime.staticCompositionLocalOf

object ProviderSession {
    val connections = staticCompositionLocalOf<List<MusicProviderConnection>> { emptyList() }
    /** Called after a provider is linked or unlinked, so the session can refresh what depends on it. */
    val onProviderChanged = staticCompositionLocalOf<() -> Unit> { {} }
}
