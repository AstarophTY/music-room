package com.musicroom.app.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

/** Row of the side menu (large screens). Events is always yellow, like the disc of the bottom bar. [collapsed]: icon only. */
@Composable
fun SideNavItem(tab: MainTab, selected: Boolean, collapsed: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val isEvents = tab == MainTab.Events
    val background = when {
        isEvents -> colors.accent
        selected -> colors.accentSubtle
        else -> Color.Transparent
    }
    val content = when {
        isEvents -> colors.onAccent
        selected -> colors.accentText
        else -> colors.textSecondary
    }
    val spin = rememberInfiniteTransition()
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4_000, easing = LinearEasing), RepeatMode.Restart),
    )
    Row(
        Modifier.fillMaxWidth().pressable(onClick = onClick).background(background, AppShapes.pill)
            .padding(horizontal = if (collapsed) 0.dp else AppTheme.spacing.md, vertical = AppTheme.spacing.sm + 2.dp),
        horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.spacedBy(AppTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            tab.icon,
            contentDescription = if (collapsed) stringResource(tab.label) else null,
            tint = content,
            modifier = Modifier.size(22.dp).rotate(if (isEvents && selected) angle else 0f),
        )
        if (!collapsed) Text(stringResource(tab.label), style = AppTheme.typography.label, color = content, maxLines = 1, softWrap = false)
    }
}
