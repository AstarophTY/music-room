package com.musicroom.app.player

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.SkipBack
import com.composables.icons.lucide.SkipForward
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.player_next
import com.musicroom.app.generated.resources.player_previous
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

/** Next track ([forward]) or previous one; greyed out when the focused event does not let the user ([PlayerController.canSkipNext]). */
@Composable
fun SkipButton(player: PlayerController, forward: Boolean) {
    val enabled = if (forward) player.canSkipNext else player.canSkipPrevious
    IconButton(onClick = if (forward) player::next else player::previous, enabled = enabled) {
        Icon(
            if (forward) Lucide.SkipForward else Lucide.SkipBack,
            contentDescription = stringResource(if (forward) Res.string.player_next else Res.string.player_previous),
            tint = if (enabled) Color.White else AppTheme.colors.textDisabled,
        )
    }
}
