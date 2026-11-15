package com.musicroom.app.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.player_pause
import com.musicroom.app.generated.resources.player_play
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Round play / pause button in the accent color. While an event is focused it shows the event's state and drives it;
 * greyed out when the user cannot ([PlayerController.canTogglePlay]).
 */
@Composable
fun PlayButton(player: PlayerController, size: Dp) {
    val colors = AppTheme.colors
    val enabled = player.canTogglePlay
    val playing = player.showsPlaying
    Box(
        Modifier.size(size).background(if (enabled) colors.accent else colors.surfaceSubtle, CircleShape).pressable(enabled, player::togglePlay),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (playing) Lucide.Pause else Lucide.Play,
            contentDescription = stringResource(if (playing) Res.string.player_pause else Res.string.player_play),
            tint = if (enabled) colors.onAccent else colors.textDisabled,
            modifier = Modifier.size(size * 0.45f),
        )
    }
}
