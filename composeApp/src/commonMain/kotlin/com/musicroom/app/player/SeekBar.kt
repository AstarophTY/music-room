package com.musicroom.app.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musicroom.app.music.Durations
import com.musicroom.app.theme.tokens.AppTheme

/** Elapsed time, a slider to move in the track (applied when released), and the track length. */
@Composable
fun SeekBar(player: PlayerController, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val position by rememberPlaybackPosition(player)
    val duration = player.track?.durationMs ?: 0
    val fraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    var dragged by remember { mutableStateOf<Float?>(null) }
    Row(
        modifier,
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
