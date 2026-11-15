package com.musicroom.app.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Disc3
import com.composables.icons.lucide.Headphones
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MapPin
import com.composables.icons.lucide.Vote
import com.musicroom.app.common.PersonAvatar
import com.musicroom.app.common.RemoteImage
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.event_by
import com.musicroom.app.generated.resources.event_live
import com.musicroom.app.generated.resources.event_paused
import com.musicroom.app.generated.resources.event_queued_count
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.EventResponse
import com.musicroom.shared.dto.EventStatus
import com.musicroom.shared.dto.EventVisibility
import com.musicroom.shared.dto.EventVoteLicense
import org.jetbrains.compose.resources.stringResource

/**
 * An event in a list: the cover of the song playing (a disc otherwise), its name and owner, a "live" badge while
 * the music plays, and how many songs wait and how many people follow it. [distance] (e.g. "300 m") for nearby ones.
 */
@Composable
fun EventCard(api: MusicRoomApi, event: EventResponse, onClick: () -> Unit, modifier: Modifier = Modifier, distance: String? = null) {
    val colors = AppTheme.colors
    val playing = event.status == EventStatus.PLAYING
    Row(
        modifier.fillMaxWidth().pressable(onClick = onClick)
            .background(if (playing) colors.accentSubtle else colors.surface, AppShapes.large)
            .padding(AppTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val cover = event.nowPlaying?.imageUrl
        if (cover != null) {
            RemoteImage(api, cover, Modifier.size(64.dp).clip(AppShapes.medium))
        } else {
            Box(Modifier.size(64.dp).background(colors.surfaceSubtle, AppShapes.medium), contentAlignment = Alignment.Center) {
                Icon(Lucide.Disc3, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(30.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                if (event.visibility == EventVisibility.PRIVATE) {
                    Icon(Lucide.Lock, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                }
                Text(event.name, style = AppTheme.typography.heading, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (event.status != EventStatus.WAITING) {
                    Row(
                        Modifier.background(if (playing) colors.accent else colors.surfaceSubtle, AppShapes.pill)
                            .padding(horizontal = AppTheme.spacing.sm, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(if (playing) Res.string.event_live else Res.string.event_paused),
                            style = AppTheme.typography.bodySmall,
                            color = if (playing) colors.onAccent else colors.textSecondary,
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                PersonAvatar(api, event.owner.id, event.owner.displayName, event.owner.avatarColor, event.owner.avatarVersion, size = 16.dp)
                Text(stringResource(Res.string.event_by, event.owner.displayName), style = AppTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            event.nowPlaying?.let {
                Text("♪ ${it.name} · ${it.artists.joinToString(", ")}", style = AppTheme.typography.bodySmall, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md), verticalAlignment = Alignment.CenterVertically) {
                val info: @Composable (androidx.compose.ui.graphics.vector.ImageVector, String) -> Unit = { icon, text ->
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(12.dp))
                        Text(text, style = AppTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
                info(Lucide.Vote, stringResource(Res.string.event_queued_count, event.queuedCount))
                info(Lucide.Headphones, event.listeners.toString())
                if (event.voteLicense == EventVoteLicense.LOCATION || distance != null) info(Lucide.MapPin, distance ?: "")
            }
        }
    }
}
