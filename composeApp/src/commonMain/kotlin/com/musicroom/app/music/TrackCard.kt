package com.musicroom.app.music

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
import com.musicroom.app.common.RemoteImage
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.TrackResponse

/** Big square cover with the title and artists under it, for rows of recommendations. */
@Composable
fun TrackCard(
    api: MusicRoomApi,
    track: TrackResponse,
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 164.dp,
) {
    val colors = AppTheme.colors
    Column(
        modifier.width(size).pressable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        RemoteImage(api, track.imageUrl, Modifier.size(size).clip(AppShapes.large))
        Text(
            track.name,
            style = AppTheme.typography.label,
            color = if (playing) colors.accentText else colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            track.artists.joinToString(", "),
            style = AppTheme.typography.bodySmall,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        ProviderIcon(track.source)
    }
}
