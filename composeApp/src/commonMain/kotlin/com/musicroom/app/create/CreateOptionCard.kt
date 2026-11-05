package com.musicroom.app.create

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
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_soon
import com.musicroom.app.theme.components.AppCard
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

/** One thing the user can create. Without [onClick] it is marked "Soon" and greyed out. */
@Composable
fun CreateOptionCard(icon: ImageVector, title: String, body: String, onClick: (() -> Unit)? = null) {
    val colors = AppTheme.colors
    val available = onClick != null
    AppCard(Modifier.fillMaxWidth().pressable(enabled = available, onClick = { onClick?.invoke() })) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).background(if (available) colors.accent else colors.surfaceSubtle, AppShapes.medium),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = if (available) colors.onAccent else colors.textDisabled, modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = AppTheme.typography.heading, color = if (available) colors.textPrimary else colors.textSecondary)
                Text(body, style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            }
            if (available) {
                Icon(Lucide.ChevronRight, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
            } else {
                Text(
                    stringResource(Res.string.common_soon),
                    modifier = Modifier.background(colors.surfaceSubtle, AppShapes.pill)
                        .padding(horizontal = AppTheme.spacing.sm, vertical = 2.dp),
                    style = AppTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}
