package com.musicroom.app.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musicroom.app.common.Avatar
import com.musicroom.app.common.Equalizer
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.app_name
import com.musicroom.app.generated.resources.profile_open
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.dto.UserResponse
import org.jetbrains.compose.resources.stringResource

/** Side menu that replaces the bottom bar on large screens. The profile stays at the top left. */
@Composable
fun SideNavBar(user: UserResponse, selected: MainTab, onSelect: (MainTab) -> Unit, onOpenProfile: () -> Unit) {
    val colors = AppTheme.colors
    val border = AppTheme.spacing.border
    Column(
        Modifier.width(256.dp).fillMaxHeight().background(colors.surface)
            .drawBehind { drawLine(colors.border, Offset(size.width, 0f), Offset(size.width, size.height), border.toPx()) }
            .safeDrawingPadding()
            .padding(AppTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        Row(
            Modifier.fillMaxWidth().pressable(onClick = onOpenProfile).padding(vertical = AppTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(user.displayName, size = 44.dp)
            Column {
                Text(user.displayName, style = AppTheme.typography.label, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(Res.string.profile_open), style = AppTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 1)
            }
        }
        Spacer(Modifier.padding(top = AppTheme.spacing.md))
        MainTab.entries.forEach { tab -> SideNavItem(tab, selected = tab == selected, onClick = { onSelect(tab) }) }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.padding(AppTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Equalizer(color = colors.accentText, height = 18.dp)
            Text(stringResource(Res.string.app_name), style = AppTheme.typography.label, color = colors.textSecondary)
        }
    }
}
