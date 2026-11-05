package com.musicroom.app.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.musicroom.app.social.CountBadge
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme

/**
 * Row of the side menu that opens a page (messages, notifications) rather than a tab, like [SideNavItem], with
 * the number of what waits there. [collapsed]: icon only, the number on its corner.
 */
@Composable
fun SideNavAction(icon: ImageVector, label: String, count: Int, selected: Boolean, collapsed: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val content = if (selected) colors.accentText else colors.textSecondary
    Row(
        Modifier.fillMaxWidth().pressable(onClick = onClick)
            .background(if (selected) colors.accentSubtle else Color.Transparent, AppShapes.pill)
            .padding(horizontal = if (collapsed) 0.dp else AppTheme.spacing.md, vertical = AppTheme.spacing.sm + 2.dp),
        horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Icon(icon, contentDescription = if (collapsed) label else null, tint = content, modifier = Modifier.size(22.dp))
            if (collapsed) CountBadge(count, Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp))
        }
        if (!collapsed) {
            Text(label, style = AppTheme.typography.label, color = content, maxLines = 1, softWrap = false, modifier = Modifier.weight(1f))
            CountBadge(count)
        }
    }
}
