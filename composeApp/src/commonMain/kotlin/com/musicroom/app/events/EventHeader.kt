package com.musicroom.app.events

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.UserPlus
import com.musicroom.app.common.WindowSize
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_back
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.event_invite_title
import com.musicroom.app.generated.resources.event_settings
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.dto.EventResponse
import org.jetbrains.compose.resources.stringResource

/**
 * Top bar of an open event, replacing the tab's: back arrow, the event's name, and its two actions (invite and
 * settings) for the owner. While the event loads, the name shows a loading label.
 */
@Composable
fun EventHeader(
    event: EventResponse?,
    onBack: () -> Unit,
    onInvite: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Row(
        modifier.fillMaxWidth()
            .then(if (WindowSize.current == WindowSize.Compact) Modifier.statusBarsPadding() else Modifier)
            .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Lucide.ArrowLeft, contentDescription = stringResource(Res.string.common_back), tint = colors.textPrimary)
        }
        Text(
            event?.name ?: stringResource(Res.string.common_loading),
            Modifier.weight(1f),
            style = AppTheme.typography.title,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (event?.isOwner == true) {
            IconButton(onClick = onInvite) {
                Icon(Lucide.UserPlus, contentDescription = stringResource(Res.string.event_invite_title), tint = colors.textPrimary)
            }
            IconButton(onClick = onSettings) {
                Icon(Lucide.Settings, contentDescription = stringResource(Res.string.event_settings), tint = colors.textPrimary)
            }
        }
    }
}
