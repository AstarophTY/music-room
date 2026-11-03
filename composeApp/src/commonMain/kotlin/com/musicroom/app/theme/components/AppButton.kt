package com.musicroom.app.theme.components

import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun AppButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, variant: AppButtonVariant = AppButtonVariant.Primary) {
    val colors = AppTheme.colors
    val background = when (variant) {
        AppButtonVariant.Primary -> colors.accent
        AppButtonVariant.Secondary -> colors.surface
        AppButtonVariant.Ghost -> colors.background
    }
    val textColor = if (!enabled) colors.textDisabled else if (variant == AppButtonVariant.Primary) colors.onAccent else colors.textPrimary
    val shape = AppShapes.medium
    Box(
        modifier = modifier.pressable(enabled, onClick).background(background, shape)
            .then(if (variant == AppButtonVariant.Secondary) Modifier.border(AppTheme.spacing.border, colors.border, shape) else Modifier)
            .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = AppTheme.typography.label, color = textColor) }
}
