package com.musicroom.app.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

/** One regular tab of the bottom bar: icon in a pill (highlighted when selected) and label. */
@Composable
fun NavItem(tab: MainTab, selected: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val tint by animateColorAsState(if (selected) colors.accentText else colors.textSecondary)
    val pill by animateColorAsState(if (selected) colors.accentSubtle else colors.surface)
    Column(
        Modifier.pressable(onClick = onClick).padding(vertical = AppTheme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(Modifier.width(52.dp).height(30.dp).background(pill, AppShapes.pill), contentAlignment = Alignment.Center) {
            Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Text(stringResource(tab.label), style = AppTheme.typography.bodySmall, color = tint, maxLines = 1)
    }
}
