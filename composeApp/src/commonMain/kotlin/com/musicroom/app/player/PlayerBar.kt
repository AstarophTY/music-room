package com.musicroom.app.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.musicroom.app.common.RemoteImage
import com.musicroom.app.common.WindowSize
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_close
import com.musicroom.app.generated.resources.event_nothing_playing
import com.musicroom.app.generated.resources.now_playing_open
import com.musicroom.app.generated.resources.player_open_app
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import org.jetbrains.compose.resources.stringResource

/**
 * Now playing bar under the content. Phones: cover, title, play/pause and next, with a thin progress line.
 * Large screens: controls in the middle with a seek bar, and the remote device on the right. The device
 * button (an icon on phones) opens the list of devices to move the music to another one.
 * Tapping the cover or the title opens the player page ([onOpen]). Errors (no device, not Premium) show
 * above it, with a button to open the remote service when that fixes it.
 */
@Composable
fun PlayerBar(api: MusicRoomApi, player: PlayerController, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val track = player.track
    val error = player.error
    if (track == null && error == null && player.eventRemote == null) return
    val colors = AppTheme.colors
    val border = AppTheme.spacing.border
    val wide = WindowSize.current == WindowSize.Expanded
    val uriHandler = LocalUriHandler.current

    Column(
        modifier.fillMaxWidth().background(colors.surface)
            .drawBehind { drawLine(colors.border, Offset(0f, 0f), Offset(size.width, 0f), border.toPx()) },
    ) {
        if (error != null) {
            Row(
                Modifier.fillMaxWidth().padding(start = AppTheme.spacing.md, end = AppTheme.spacing.xs, top = AppTheme.spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(error.resolve(), style = AppTheme.typography.bodySmall, color = colors.error, modifier = Modifier.weight(1f))
                if (player.needsDevice) {
                    player.remoteAppUrl?.let { url ->
                        AppButton(
                            stringResource(Res.string.player_open_app, player.remoteDisplayName),
                            { runCatching { uriHandler.openUri(url) } },
                            variant = AppButtonVariant.Ghost,
                        )
                    }
                }
                IconButton(onClick = player::dismissError) {
                    Icon(Lucide.X, contentDescription = stringResource(Res.string.common_close), tint = colors.textSecondary)
                }
            }
        }
        if (track == null) {
            // Listening to an event that plays nothing yet: its owner can still start it from here.
            if (player.eventRemote != null) {
                Row(
                    Modifier.fillMaxWidth().padding(start = AppTheme.spacing.md, end = AppTheme.spacing.xs, top = AppTheme.spacing.sm, bottom = AppTheme.spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(Res.string.event_nothing_playing),
                        style = AppTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f).clip(AppShapes.small).pressable(onClick = onOpen),
                    )
                    PlayButton(player, 40.dp)
                    SkipButton(player, forward = true)
                }
            }
            return@Column
        }

        // Cover and titles: tapping them opens the player page.
        val summary: @Composable (Int, Modifier) -> Unit = { coverSize, summaryModifier ->
            Row(
                summaryModifier.clip(AppShapes.small).pressable(onClick = onOpen),
                horizontalArrangement = Arrangement.spacedBy(if (wide) AppTheme.spacing.md else AppTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RemoteImage(api, track.imageUrl, Modifier.size(coverSize.dp).clip(AppShapes.small))
                Column(Modifier.weight(1f)) {
                    Text(track.name, style = AppTheme.typography.label, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        track.artists.joinToString(", "),
                        style = AppTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (!wide) {
            val position by rememberPlaybackPosition(player)
            val duration = track.durationMs
            val fraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
            Box(Modifier.fillMaxWidth().height(2.dp).background(colors.border)) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(colors.accent))
            }
            Row(
                Modifier.fillMaxWidth().padding(start = AppTheme.spacing.md, end = AppTheme.spacing.xs, top = AppTheme.spacing.sm, bottom = AppTheme.spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                summary(44, Modifier.weight(1f))
                DeviceButton(player, iconOnly = true)
                PlayButton(player, 40.dp)
                SkipButton(player, forward = true)
            }
            return@Column
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            summary(56, Modifier.weight(1f))
            Column(Modifier.weight(1.4f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    SkipButton(player, forward = false)
                    PlayButton(player, 40.dp)
                    SkipButton(player, forward = true)
                }
                SeekBar(player)
            }
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DeviceButton(player, Modifier.weight(1f, fill = false))
                IconButton(onClick = onOpen) {
                    Icon(Lucide.ListMusic, contentDescription = stringResource(Res.string.now_playing_open), tint = colors.textPrimary)
                }
            }
        }
    }
}
