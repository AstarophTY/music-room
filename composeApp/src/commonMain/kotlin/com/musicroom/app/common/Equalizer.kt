package com.musicroom.app.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Animated equalizer bars, the app's visual signature. */
@Composable
fun Equalizer(color: Color, modifier: Modifier = Modifier, height: Dp = 24.dp, bars: Int = 5) {
    val transition = rememberInfiniteTransition()
    val levels = (0 until bars).map { index ->
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = InfiniteRepeatableSpec(
                animation = tween(durationMillis = 420 + index * 130 % 400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        )
    }
    Canvas(modifier.size(width = height * bars * 0.5f, height = height)) {
        val gap = size.width / (bars * 2 - 1)
        levels.forEachIndexed { index, level ->
            val barHeight = size.height * level.value
            drawRoundRect(
                color = color,
                topLeft = Offset(index * gap * 2, size.height - barHeight),
                size = Size(gap, barHeight),
                cornerRadius = CornerRadius(gap / 2),
            )
        }
    }
}
