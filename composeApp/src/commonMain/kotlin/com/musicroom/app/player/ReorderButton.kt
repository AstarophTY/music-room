package com.musicroom.app.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowUpDown
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.queue_reorder
import com.musicroom.app.generated.resources.queue_reorder_done
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

/**
 * "Change the order" / "Done" pill of the queue. Switching fills it with the accent color, spins the icon
 * into a check mark and rolls the label to the new one.
 */
@Composable
fun ReorderButton(reordering: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val background by animateColorAsState(if (reordering) colors.accent else Color.Transparent)
    val content by animateColorAsState(if (reordering) colors.onAccent else Color.White)
    val turn by animateFloatAsState(
        if (reordering) 180f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
    )
    Row(
        modifier.defaultMinSize(minHeight = 40.dp).pressable(onClick = onClick).background(background, AppShapes.pill)
            .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            reordering,
            transitionSpec = { (scaleIn(initialScale = 0.4f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.4f) + fadeOut()) },
            modifier = Modifier.graphicsLayer { rotationZ = turn },
        ) { done ->
            Icon(if (done) Lucide.Check else Lucide.ArrowUpDown, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        }
        AnimatedContent(
            reordering,
            transitionSpec = {
                val down = if (targetState) 1 else -1
                (slideInVertically { it * down } + fadeIn()) togetherWith (slideOutVertically { -it * down } + fadeOut()) using
                    SizeTransform(clip = false)
            },
        ) { done ->
            Text(
                stringResource(if (done) Res.string.queue_reorder_done else Res.string.queue_reorder),
                style = AppTheme.typography.label,
                color = content,
            )
        }
    }
}
