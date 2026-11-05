package com.musicroom.app.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme

/** Selectable pill, e.g. a filter. */
@Composable
fun AppChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Text(
        text,
        modifier = modifier.pressable(onClick = onClick)
            .background(if (selected) colors.accent else colors.surface, AppShapes.pill)
            .border(AppTheme.spacing.border, if (selected) colors.accent else colors.border, AppShapes.pill)
            .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
        style = AppTheme.typography.label,
        color = if (selected) colors.onAccent else colors.textPrimary,
    )
}
