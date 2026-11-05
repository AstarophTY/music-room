package com.musicroom.app.music

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ListPlus
import com.composables.icons.lucide.Lucide
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.queue_add
import com.musicroom.app.theme.tokens.AppTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * Lets [content] be swiped to the right to add the song to the queue ([onQueue]): an accent band with the
 * queue icon follows the finger, and past the threshold, releasing adds it. The row slides back either way.
 */
@Composable
fun SwipeToQueue(onQueue: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val density = LocalDensity.current
    val threshold = with(density) { 96.dp.toPx() }
    val max = with(density) { 140.dp.toPx() }
    val radius = with(density) { 8.dp.toPx() }
    val progress = (offset.value / threshold).coerceIn(0f, 1f)

    Box(
        modifier
            .drawBehind {
                if (offset.value > 0f) {
                    drawRoundRect(
                        if (progress >= 1f) colors.accent else colors.accent.copy(alpha = 0.35f + 0.4f * progress),
                        size = Size(offset.value, size.height),
                        cornerRadius = CornerRadius(radius),
                    )
                }
            }
            .draggable(
                rememberDraggableState { delta -> scope.launch { offset.snapTo((offset.value + delta).coerceIn(0f, max)) } },
                Orientation.Horizontal,
                onDragStopped = {
                    if (offset.value >= threshold) onQueue()
                    offset.animateTo(0f)
                },
            ),
    ) {
        if (offset.value > 0f) {
            Icon(
                Lucide.ListPlus,
                contentDescription = stringResource(Res.string.queue_add),
                tint = colors.onAccent,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = AppTheme.spacing.md).alpha(progress),
            )
        }
        Box(Modifier.offset { IntOffset(offset.value.roundToInt(), 0) }) { content() }
    }
}
