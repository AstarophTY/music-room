package com.musicroom.app.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MicVocal
import com.musicroom.app.common.RemoteImage
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.artist_label
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.ArtistResponse
import com.musicroom.shared.dto.TrackSource
import org.jetbrains.compose.resources.stringResource

/** Round photo of an artist (a microphone when no image is available) with their name and "Artist" under it. */
@Composable
fun ArtistCard(api: MusicRoomApi, artist: ArtistResponse, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Column(
        modifier.pressable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape).background(colors.surfaceSubtle),
            contentAlignment = Alignment.Center,
        ) {
            if (artist.imageUrl != null) {
                RemoteImage(api, artist.imageUrl, Modifier.fillMaxSize())
            } else {
                Icon(Lucide.MicVocal, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(40.dp))
            }
        }
        Text(
            artist.name,
            style = AppTheme.typography.label,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(stringResource(Res.string.artist_label), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
        ProviderIcon(TrackSource.fromId(artist.id))
    }
}
