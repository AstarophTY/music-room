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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musicroom.app.common.RemoteImage
import com.musicroom.app.events.EventFocus
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.TrackResponse

/**
 * Track in a list: cover, title, artists, and its length.
 * [trailing] adds an action at the end of the row (add, remove...). With [onSwipeRight], swiping the row
 * to the right adds the song to the queue, or proposes it to the focused event ([EventFocus]) when there is one.
 */
@Composable
fun TrackRow(
    api: MusicRoomApi,
    track: TrackResponse,
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    upNextList: Boolean = false
) {
    if (onSwipeRight != null) {
        val focus = EventFocus.Local.current
        val toEvent = focus?.event != null
        SwipeToAdd({ if (focus != null && toEvent) focus.propose(track) else onSwipeRight() }, modifier, toEvent) {
            TrackRow(api, track, playing, onClick, trailing = trailing, upNextList = upNextList)
        }
        return
    }
    val colors = AppTheme.colors
    Row(
        modifier.fillMaxWidth().pressable(onClick = onClick).padding(vertical = AppTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RemoteImage(api, track.imageUrl, Modifier.size(48.dp).clip(AppShapes.small))
        Column(Modifier.weight(1f)) {
            Text(
                track.name,
                style = AppTheme.typography.label,
                color = if (upNextList) Color.White else (if (playing) colors.accentText else colors.textPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                track.artists.joinToString(", "),
                style = AppTheme.typography.bodySmall,
                color = if (upNextList) Color.White else colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ProviderIcon(track.source, tint = if (upNextList) Color.White else null)
        Text(Durations.format(track.durationMs), style = AppTheme.typography.bodySmall, color = if (upNextList) Color.White else colors.textDisabled)
        trailing?.invoke()
    }
}
