package com.musicroom.app.music

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musicroom.app.common.Equalizer
import com.musicroom.app.common.RemoteImage
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.TrackResponse

/** Track in a list: cover, title, artists, and its length (an equalizer instead while it plays). */
@Composable
fun TrackRow(api: MusicRoomApi, track: TrackResponse, playing: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        Modifier.fillMaxWidth().pressable(onClick = onClick).padding(vertical = AppTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RemoteImage(api, track.imageUrl, Modifier.size(48.dp).clip(AppShapes.small))
        Column(Modifier.weight(1f)) {
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
        }
        if (playing) {
            Equalizer(color = colors.accentText, height = 16.dp)
        } else {
            Text(Durations.format(track.durationMs), style = AppTheme.typography.bodySmall, color = colors.textDisabled)
        }
    }
}
