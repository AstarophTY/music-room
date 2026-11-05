package com.musicroom.app.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme

/**
 * Category of the settings side menu (large screens), sized like the rows of the main side menu.
 * [selected]: its page is shown on the right; the highlight fades in and the icon grows a little.
 */
@Composable
fun SettingsSideItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit, destructive: Boolean = false) {
    val colors = AppTheme.colors
    val content by animateColorAsState(
        when {
            destructive -> colors.error
            selected -> colors.accentText
            else -> colors.textSecondary
        },
    )
    val background by animateColorAsState(if (selected) colors.accentSubtle else Color.Transparent)
    val iconScale by animateFloatAsState(
        if (selected) 1.15f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
    )
    Row(
        Modifier.fillMaxWidth().pressable(onClick = onClick)
            .background(background, AppShapes.pill)
            .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm + 2.dp),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(22.dp).graphicsLayer {
                scaleX = iconScale
                scaleY = iconScale
            },
        )
        Text(label, style = AppTheme.typography.label, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
