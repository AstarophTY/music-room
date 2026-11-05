package com.musicroom.app.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
 * [destructive] shows it in red (e.g. log out). A changed value slides in; the check mark pops in.
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
    // The check mark pops in when the row gets selected.
    val checkScale = remember { Animatable(if (selected) 1f else 0f) }
    LaunchedEffect(selected) {
        if (selected) {
            checkScale.snapTo(0f)
            checkScale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f))
        }
    }
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
            if (value != null) {
                // A new value (2FA turned on, another theme...) slides in over the old one.
                AnimatedContent(
                    value,
                    transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
                ) { shown ->
                    Text(shown, style = AppTheme.typography.bodySmall, color = colors.textSecondary)
                }
            }
        }
        when {
            selected -> Icon(
                Lucide.Check,
                contentDescription = null,
                tint = colors.accentText,
                modifier = Modifier.size(20.dp).graphicsLayer {
                    scaleX = checkScale.value
                    scaleY = checkScale.value
                },
            )
            showChevron -> Icon(Lucide.ChevronRight, contentDescription = null, tint = colors.textDisabled, modifier = Modifier.size(18.dp))
        }
    }
}
