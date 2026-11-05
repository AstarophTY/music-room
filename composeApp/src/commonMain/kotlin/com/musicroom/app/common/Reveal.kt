package com.musicroom.app.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Shows [content] with a fade and a small rise when the page opens, [index] steps after the first one
 * (so the parts of a page arrive one after the other).
 */
@Composable
fun Reveal(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    val delay = index * STEP_MS
    AnimatedVisibility(
        state,
        modifier,
        enter = fadeIn(tween(320, delayMillis = delay)) +
            slideInVertically(tween(420, delayMillis = delay, easing = FastOutSlowInEasing)) { it / 3 },
    ) { content() }
}

private const val STEP_MS = 55
