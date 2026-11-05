package com.musicroom.app.events

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme

/** Selectable card with an icon, a title and what it means (like the playlist types). Lights up when [selected]. */
@Composable
fun ChoiceOption(
    icon: ImageVector,
    title: String,
    body: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    val border by animateColorAsState(if (selected) colors.accent else colors.border)
    val iconBackground by animateColorAsState(if (selected) colors.accent else colors.surfaceSubtle)
    val iconTint by animateColorAsState(if (selected) colors.onAccent else colors.textSecondary)
    Row(
        modifier.fillMaxWidth().pressable(enabled, onClick)
            .background(if (selected) colors.accentSubtle else colors.surface, AppShapes.large)
            .border(if (selected) 2.dp else AppTheme.spacing.border, border, AppShapes.large)
            .padding(AppTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).background(iconBackground, AppShapes.medium), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = AppTheme.typography.label, color = if (enabled) colors.textPrimary else colors.textDisabled)
            Text(body, style = AppTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
}
