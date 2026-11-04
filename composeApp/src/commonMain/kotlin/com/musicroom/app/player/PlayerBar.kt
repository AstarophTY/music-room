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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.SkipBack
import com.composables.icons.lucide.SkipForward
import com.composables.icons.lucide.Speaker
import com.composables.icons.lucide.X
import com.musicroom.app.common.RemoteImage
import com.musicroom.app.common.WindowSize
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_close
import com.musicroom.app.generated.resources.player_next
import com.musicroom.app.generated.resources.player_on_device
import com.musicroom.app.generated.resources.player_pause
import com.musicroom.app.generated.resources.player_play
import com.musicroom.app.generated.resources.player_previous
import com.musicroom.app.generated.resources.spotify_open
import com.musicroom.app.music.Durations
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * Now playing bar under the content. Phones: cover, title, play/pause and next, with a thin progress line.
 * Large screens: controls in the middle with a seek bar, and the Spotify device on the right.
 * Errors (no device, not Premium) show above it, with a button to open Spotify when that fixes it.
 */
@Composable
fun PlayerBar(api: MusicRoomApi, player: PlayerController, modifier: Modifier = Modifier) {
    val track = player.track
    val error = player.error
    if (track == null && error == null) return
    val colors = AppTheme.colors
    val border = AppTheme.spacing.border
    val wide = WindowSize.current == WindowSize.Expanded
    val uriHandler = LocalUriHandler.current
    val position by produceState(player.positionMs(), player.state) {
        while (true) {
            value = player.positionMs()
            delay(500)
        }
    }
    val duration = track?.durationMs ?: 0
    val fraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f

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
                    AppButton(
                        stringResource(Res.string.spotify_open),
                        { runCatching { uriHandler.openUri(PlayerController.SPOTIFY_WEB_URL) } },
                        variant = AppButtonVariant.Ghost,
                    )
                }
                IconButton(onClick = player::dismissError) {
                    Icon(Lucide.X, contentDescription = stringResource(Res.string.common_close), tint = colors.textSecondary)
                }
            }
        }
        if (track == null) return@Column

        val cover: @Composable (Int) -> Unit = { size -> RemoteImage(api, track.imageUrl, Modifier.size(size.dp).clip(AppShapes.small)) }
        val titles: @Composable (Modifier) -> Unit = { textModifier ->
            Column(textModifier) {
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
        val playButton: @Composable (Int) -> Unit = { size ->
            val label = stringResource(if (player.isPlaying) Res.string.player_pause else Res.string.player_play)
            Box(
                Modifier.size(size.dp).background(colors.accent, CircleShape).pressable(onClick = player::togglePlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (player.isPlaying) Lucide.Pause else Lucide.Play,
                    contentDescription = label,
                    tint = colors.onAccent,
                    modifier = Modifier.size((size * 0.45f).dp),
                )
            }
        }
        val skip: @Composable (Boolean) -> Unit = { forward ->
            IconButton(onClick = if (forward) player::next else player::previous) {
                Icon(
                    if (forward) Lucide.SkipForward else Lucide.SkipBack,
                    contentDescription = stringResource(if (forward) Res.string.player_next else Res.string.player_previous),
                    tint = colors.textPrimary,
                )
            }
        }

        if (!wide) {
            Box(Modifier.fillMaxWidth().height(2.dp).background(colors.border)) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(colors.accent))
            }
            Row(
                Modifier.fillMaxWidth().padding(start = AppTheme.spacing.md, end = AppTheme.spacing.xs, top = AppTheme.spacing.sm, bottom = AppTheme.spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                cover(44)
                titles(Modifier.weight(1f))
                playButton(40)
                skip(true)
            }
            return@Column
        }

        var dragged by remember { mutableStateOf<Float?>(null) }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                cover(56)
                titles(Modifier.weight(1f))
            }
            Column(Modifier.weight(1.4f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    skip(false)
                    playButton(40)
                    skip(true)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val shown = dragged?.let { (it * duration).toLong() } ?: position
                    Text(Durations.format(shown), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
                    Slider(
                        value = dragged ?: fraction,
                        onValueChange = { dragged = it },
                        onValueChangeFinished = {
                            dragged?.let { player.seek((it * duration).toLong()) }
                            dragged = null
                        },
                        modifier = Modifier.weight(1f).height(24.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = colors.accent,
                            activeTrackColor = colors.accent,
                            inactiveTrackColor = colors.border,
                        ),
                    )
                    Text(Durations.format(duration), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
                }
            }
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                player.state?.deviceName?.let { device ->
                    Icon(Lucide.Speaker, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(16.dp))
                    Text(
                        stringResource(Res.string.player_on_device, device),
                        style = AppTheme.typography.bodySmall,
                        color = colors.accentText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
