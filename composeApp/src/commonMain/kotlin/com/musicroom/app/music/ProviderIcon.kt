package com.musicroom.app.music

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.apple_music_logo
import com.musicroom.app.generated.resources.spotify_logo
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.dto.TrackSource
import org.jetbrains.compose.resources.painterResource

@Composable
fun ProviderIcon(source: TrackSource) {
    when (source) {
        TrackSource.SPOTIFY -> Image(painterResource(Res.drawable.spotify_logo), "Spotify", Modifier.size(16.dp))
        TrackSource.APPLE_MUSIC -> Image(painterResource(Res.drawable.apple_music_logo), "Apple Music", Modifier.size(16.dp), colorFilter = ColorFilter.tint(AppTheme.colors.textPrimary))
        TrackSource.MUSIC_ROOM -> Unit
    }
}
