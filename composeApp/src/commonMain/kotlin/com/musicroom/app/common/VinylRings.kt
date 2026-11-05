package com.musicroom.app.common

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Faint concentric grooves of a vinyl record, centered near the top right corner. Decoration only. */
@Composable
fun VinylRings(color: Color, modifier: Modifier = Modifier, rings: Int = 7, spacing: Dp = 22.dp) {
    Canvas(modifier.clipToBounds()) {
        val center = Offset(size.width * 0.95f, size.height * 0.1f)
        for (ring in 1..rings) {
            drawCircle(
                color = color.copy(alpha = 0.07f),
                radius = ring * spacing.toPx(),
                center = center,
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }
    }
}
