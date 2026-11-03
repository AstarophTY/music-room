package com.musicroom.app.profile

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme

/**
 * Menu row: icon, label, optional value, then a chevron (or a check mark when [selected]).
 * [destructive] shows it in red (e.g. log out).
 */
@Composable
fun SettingsRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    value: String? = null,
    destructive: Boolean = false,
    showChevron: Boolean = true,
    selected: Boolean = false,
) {
    val colors = AppTheme.colors
    val tint = if (destructive) colors.error else colors.textPrimary
    Row(
        Modifier.fillMaxWidth().pressable(onClick = onClick).padding(vertical = AppTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).background(if (destructive) colors.surfaceSubtle else colors.accentSubtle, AppShapes.medium),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (destructive) colors.error else colors.accentText, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = AppTheme.typography.label, color = tint)
            if (value != null) Text(value, style = AppTheme.typography.bodySmall, color = colors.textSecondary)
        }
        when {
            selected -> Icon(Lucide.Check, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(20.dp))
            showChevron -> Icon(Lucide.ChevronRight, contentDescription = null, tint = colors.textDisabled, modifier = Modifier.size(18.dp))
        }
    }
}
