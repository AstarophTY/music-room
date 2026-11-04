package com.musicroom.app.create

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Users
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.playlist_type_personal
import com.musicroom.app.generated.resources.playlist_type_personal_body
import com.musicroom.app.generated.resources.playlist_type_shared
import com.musicroom.app.generated.resources.playlist_type_shared_body
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.dto.PlaylistType
import org.jetbrains.compose.resources.stringResource

/** Selectable card for a playlist type, with what it means. The border and icon light up when selected. */
@Composable
fun PlaylistTypeOption(type: PlaylistType, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = AppTheme.colors
    val border by animateColorAsState(if (selected) colors.accent else colors.border)
    val iconBackground by animateColorAsState(if (selected) colors.accent else colors.surfaceSubtle)
    val iconTint by animateColorAsState(if (selected) colors.onAccent else colors.textSecondary)
    val shared = type == PlaylistType.SHARED
    Row(
        modifier.fillMaxWidth().pressable(enabled, onClick)
            .background(if (selected) colors.accentSubtle else colors.surface, AppShapes.large)
            .border(if (selected) 2.dp else AppTheme.spacing.border, border, AppShapes.large)
            .padding(AppTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).background(iconBackground, AppShapes.medium), contentAlignment = Alignment.Center) {
            Icon(if (shared) Lucide.Users else Lucide.Lock, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(if (shared) Res.string.playlist_type_shared else Res.string.playlist_type_personal),
                style = AppTheme.typography.label,
                color = colors.textPrimary,
            )
            Text(
                stringResource(if (shared) Res.string.playlist_type_shared_body else Res.string.playlist_type_personal_body),
                style = AppTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
    }
}
