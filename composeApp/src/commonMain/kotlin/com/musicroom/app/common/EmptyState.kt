package com.musicroom.app.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.components.AppCard
import com.musicroom.app.theme.tokens.AppTheme

/** Placeholder card shown when a list has nothing to show yet. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val colors = AppTheme.colors
    AppCard(modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            Box(Modifier.size(56.dp).background(colors.accentSubtle, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(26.dp))
            }
            Text(title, style = AppTheme.typography.heading, color = colors.textPrimary, textAlign = TextAlign.Center)
            Text(body, style = AppTheme.typography.bodySmall, color = colors.textSecondary, textAlign = TextAlign.Center)
            if (actionLabel != null) {
                AppButton(actionLabel, onAction, Modifier.padding(top = AppTheme.spacing.xs), variant = AppButtonVariant.Secondary)
            }
        }
    }
}
