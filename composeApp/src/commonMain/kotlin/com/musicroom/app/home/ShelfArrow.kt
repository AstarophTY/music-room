package com.musicroom.app.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppTheme

/** Round arrow button that scrolls a row of tracks with the mouse. */
@Composable
fun ShelfArrow(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(colors.surface)
            .border(AppTheme.spacing.border, colors.border, CircleShape)
            .pressable(enabled, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) colors.textPrimary else colors.textDisabled,
            modifier = Modifier.size(18.dp),
        )
    }
}
