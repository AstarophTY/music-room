package com.musicroom.app.music

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.apple_music_logo
import com.musicroom.app.generated.resources.spotify_logo
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.dto.TrackSource
import org.jetbrains.compose.resources.painterResource

/** The service's logo; [tint] overrides the logo's own color where the artwork allows it. */
@Composable
fun ProviderIcon(source: TrackSource, tint: Color? = null, size: Dp = 16.dp) {
    when (source) {
        // The Spotify artwork is a filled disc: tinting it would erase its waves, so it keeps its brand color.
        TrackSource.SPOTIFY -> Image(painterResource(Res.drawable.spotify_logo), "Spotify", Modifier.size(size))
        TrackSource.APPLE_MUSIC -> Image(
            painterResource(Res.drawable.apple_music_logo),
            "Apple Music",
            Modifier.size(size),
            colorFilter = ColorFilter.tint(tint ?: AppTheme.colors.textPrimary),
        )
        TrackSource.MUSIC_ROOM -> Unit
    }
}
