package com.musicroom.app.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.musicroom.app.common.PersonAvatar
import com.musicroom.app.common.RemoteImage
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.event_proposed_by
import com.musicroom.app.generated.resources.event_track_remove
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.EventTrackResponse
import org.jetbrains.compose.resources.stringResource

/**
 * A song of the event's queue: its place ([rank], hidden when null), cover, title, who proposed it, and [trailing]
 * (the vote button). [onRemove] (the owner, or the proposer) shows a cross.
 */
@Composable
fun EventTrackRow(
    api: MusicRoomApi,
    entry: EventTrackResponse,
    rank: Int?,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    val colors = AppTheme.colors
    Row(
        modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (rank != null) {
            Text(
                rank.toString(),
                style = AppTheme.typography.label,
                color = if (rank == 1) colors.accentText else colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(24.dp),
            )
        }
        RemoteImage(api, entry.track.imageUrl, Modifier.size(48.dp).clip(AppShapes.small))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.track.name, style = AppTheme.typography.label, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                entry.track.artists.joinToString(", "),
                style = AppTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            entry.proposedBy?.let { by ->
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    PersonAvatar(api, by.id, by.displayName, by.avatarColor, by.avatarVersion, size = 16.dp)
                    Text(
                        stringResource(Res.string.event_proposed_by, by.displayName),
                        style = AppTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(Lucide.X, contentDescription = stringResource(Res.string.event_track_remove), tint = colors.textSecondary, modifier = Modifier.size(18.dp))
            }
        }
        trailing()
    }
}
