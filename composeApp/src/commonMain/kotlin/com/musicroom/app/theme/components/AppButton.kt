package com.musicroom.app.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
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
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: AppButtonVariant = AppButtonVariant.Primary,
    icon: ImageVector? = null,
) {
    val colors = AppTheme.colors
    val background = when {
        (variant == AppButtonVariant.Primary || variant == AppButtonVariant.OnAccent) && !enabled -> colors.surfaceSubtle
        variant == AppButtonVariant.Primary -> colors.accent
        variant == AppButtonVariant.OnAccent -> colors.onAccent
        variant == AppButtonVariant.Secondary -> colors.surface
        else -> Color.Transparent
    }
    val content = when {
        !enabled -> colors.textDisabled
        variant == AppButtonVariant.Primary -> colors.onAccent
        variant == AppButtonVariant.OnAccent -> colors.accent
        else -> colors.textPrimary
    }
    val shape = AppShapes.pill
    Row(
        modifier = modifier.defaultMinSize(minHeight = 48.dp).pressable(enabled, onClick).background(background, shape)
            .then(if (variant == AppButtonVariant.Secondary) Modifier.border(AppTheme.spacing.border, colors.border, shape) else Modifier)
            .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        Text(text, style = AppTheme.typography.label, color = content)
    }
}
