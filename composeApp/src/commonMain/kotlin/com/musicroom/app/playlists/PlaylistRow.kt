package com.musicroom.app.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musicroom.app.common.RemoteImage
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.playlists_by
import com.musicroom.app.generated.resources.playlists_track_count
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.PlaylistSummaryResponse
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Playlist in a list: cover, name, then its owner and track count. */
@Composable
fun PlaylistRow(api: MusicRoomApi, playlist: PlaylistSummaryResponse, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val details = listOfNotNull(
        playlist.owner?.let { stringResource(Res.string.playlists_by, it) },
        playlist.trackCount?.let { pluralStringResource(Res.plurals.playlists_track_count, it, it) },
    ).joinToString(" · ")
    Row(
        modifier.pressable(onClick = onClick).padding(vertical = AppTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RemoteImage(api, playlist.imageUrl, Modifier.size(56.dp).clip(AppShapes.small))
        Column(Modifier.weight(1f)) {
            Text(
                playlist.name,
                style = AppTheme.typography.label,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (details.isNotEmpty()) {
                Text(
                    details,
                    style = AppTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
