package com.musicroom.app.music

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.musicroom.app.common.RemoteImage
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.AlbumResponse
import com.musicroom.shared.dto.TrackSource

/**
 * Square cover with the album name and "Album · 2001" under it. Its width comes from [modifier]
 * (a fixed width in a row, a weight in a grid). [showArtists] adds the artists to the second line.
 */
@Composable
fun AlbumCard(api: MusicRoomApi, album: AlbumResponse, onClick: () -> Unit, modifier: Modifier = Modifier, showArtists: Boolean = true) {
    val colors = AppTheme.colors
    Column(modifier.pressable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
        RemoteImage(api, album.imageUrl, Modifier.fillMaxWidth().aspectRatio(1f).clip(AppShapes.medium))
        ProviderIcon(if (album.id.startsWith("apple:")) TrackSource.APPLE_MUSIC else TrackSource.SPOTIFY)
        Text(album.name, style = AppTheme.typography.label, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            listOfNotNull(albumLabel(album), album.artists.joinToString(", ") { it.name }.takeIf { showArtists && it.isNotEmpty() })
                .joinToString(" · "),
            style = AppTheme.typography.bodySmall,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
