package com.musicroom.app.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

/** Middle tab of the bottom bar: a raised yellow disc that spins while it is selected. */
@Composable
fun EventsDisc(selected: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val spin = rememberInfiniteTransition()
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4_000, easing = LinearEasing), RepeatMode.Restart),
    )
    Column(
        // Taller than the bar on purpose: the disc sticks out above it.
        Modifier.wrapContentHeight(unbounded = true).offset(y = (-14).dp).pressable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(58.dp)
                .background(colors.accent, CircleShape)
                .border(4.dp, colors.background, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                MainTab.Events.icon,
                contentDescription = stringResource(MainTab.Events.label),
                tint = colors.onAccent,
                modifier = Modifier.size(30.dp).rotate(if (selected) angle else 0f),
            )
        }
        Text(
            stringResource(MainTab.Events.label),
            style = AppTheme.typography.bodySmall,
            color = if (selected) colors.accentText else colors.textSecondary,
            maxLines = 1,
        )
    }
}
