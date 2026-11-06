package com.musicroom.app.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.playlists_track_count
import com.musicroom.app.playlists.PlaylistCover
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.PlaylistResponse
import org.jetbrains.compose.resources.pluralStringResource

/** Square playlist card of the home page: its photo, its name and how many songs it holds. */
@Composable
fun HomePlaylistCard(
    api: MusicRoomApi,
    playlist: PlaylistResponse,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 164.dp,
) {
    val colors = AppTheme.colors
    Column(
        modifier.width(size).pressable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        PlaylistCover(
            api,
            playlist.id,
            playlist.coverVersion,
            fallbackUrl = null,
            modifier = Modifier.size(size).clip(AppShapes.large),
        )
        Text(
            playlist.name,
            style = AppTheme.typography.label,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            pluralStringResource(Res.plurals.playlists_track_count, playlist.trackCount, playlist.trackCount),
            style = AppTheme.typography.bodySmall,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
