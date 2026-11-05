package com.musicroom.app.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Cast
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Lucide
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.devices_choose
import com.musicroom.app.generated.resources.player_on_device
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Where the music plays ("Playing on Laptop", or "Choose a device"); tapping it opens the list of devices
 * to move the music to another one. [iconOnly] shows just the icon (phone player bar).
 */
@Composable
fun DeviceButton(player: PlayerController, modifier: Modifier = Modifier, iconOnly: Boolean = false) {
    val colors = AppTheme.colors
    var choosing by remember { mutableStateOf(false) }
    val device = player.state?.deviceName
    val label = device?.let { stringResource(Res.string.player_on_device, it) } ?: stringResource(Res.string.devices_choose)
    val tint = if (device != null) colors.accentText else colors.textSecondary

    if (iconOnly) {
        IconButton(onClick = { choosing = true }, modifier = modifier) {
            Icon(Lucide.Cast, contentDescription = label, tint = tint)
        }
    } else {
        Row(
            modifier.clip(AppShapes.pill).pressable(onClick = { choosing = true })
                .padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Lucide.Cast, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Text(
                label,
                style = AppTheme.typography.bodySmall,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(Lucide.ChevronDown, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        }
    }
    if (choosing) DevicePickerSheet(player, onDismiss = { choosing = false })
}
