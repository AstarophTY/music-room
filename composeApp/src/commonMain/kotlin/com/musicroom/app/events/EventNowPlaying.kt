package com.musicroom.app.events

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.musicroom.app.common.RemoteImage
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.event_nothing_playing
import com.musicroom.app.generated.resources.event_now_playing
import com.musicroom.app.generated.resources.event_paused
import com.musicroom.app.generated.resources.event_proposed_by
import com.musicroom.app.music.Durations
import com.musicroom.app.theme.components.AppCard
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.EventDetailResponse
import com.musicroom.shared.dto.EventStatus
import kotlin.time.Clock
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * The song playing in the event (on the owner's Spotify): cover, title, who proposed it, and its progress, which
 * moves on locally between the server's updates. [actions] go under it (owner controls, "listen on my Spotify").
 */
@Composable
fun EventNowPlaying(api: MusicRoomApi, detail: EventDetailResponse, actions: @Composable () -> Unit) {
    val colors = AppTheme.colors
    val entry = detail.nowPlaying
    val playing = detail.event.status == EventStatus.PLAYING
    var now by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(playing, entry?.id) {
        while (playing) {
            now = Clock.System.now().toEpochMilliseconds()
            delay(500)
        }
    }
    AppCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(if (detail.event.status == EventStatus.PAUSED) Res.string.event_paused else Res.string.event_now_playing),
                    style = AppTheme.typography.label,
                    color = colors.textSecondary,
                )
            }
            AnimatedContent(entry, contentKey = { it?.id }, transitionSpec = { (scaleIn(initialScale = 0.92f) + fadeIn()) togetherWith fadeOut() }) { shown ->
                if (shown == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(72.dp).background(colors.surfaceSubtle, AppShapes.medium), contentAlignment = Alignment.Center) {
                            Icon(Lucide.Music, contentDescription = null, tint = colors.textSecondary)
                        }
                        Text(stringResource(Res.string.event_nothing_playing), style = AppTheme.typography.body, color = colors.textSecondary)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md), verticalAlignment = Alignment.CenterVertically) {
                            RemoteImage(api, shown.track.imageUrl, Modifier.size(72.dp).clip(AppShapes.medium))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(shown.track.name, style = AppTheme.typography.heading, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(shown.track.artists.joinToString(", "), style = AppTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                shown.proposedBy?.let {
                                    Text(stringResource(Res.string.event_proposed_by, it.displayName), style = AppTheme.typography.bodySmall, color = colors.accentText, maxLines = 1)
                                }
                            }
                        }
                        val duration = shown.track.durationMs.coerceAtLeast(1)
                        val elapsed = if (playing) now - detail.sentAt else 0
                        val position = (detail.progressMs + elapsed.coerceAtLeast(0)).coerceIn(0, duration)
                        LinearProgressIndicator(
                            progress = { position.toFloat() / duration },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(AppShapes.pill),
                            color = colors.accent,
                            trackColor = colors.surfaceSubtle,
                            drawStopIndicator = {},
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(Durations.format(position), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
                            Text(Durations.format(duration), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                }
            }
            actions()
        }
    }
}
