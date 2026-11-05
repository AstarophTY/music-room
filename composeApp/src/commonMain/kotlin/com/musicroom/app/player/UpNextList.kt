package com.musicroom.app.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.composables.icons.lucide.GripVertical
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.musicroom.app.common.PersonAvatar
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.queue_drag
import com.musicroom.app.generated.resources.queue_remove
import com.musicroom.app.music.TrackMoreButton
import com.musicroom.app.music.TrackRow
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * Songs added to the queue in the app, with the profile picture of who proposed each. While [reordering],
 * each song has a handle instead: hold it, then drag the song up or down to change its place (the song
 * already handed to Spotify cannot move), and an X to remove it.
 */
@Composable
fun UpNextList(api: MusicRoomApi, player: PlayerController, reordering: Boolean) {
    val colors = AppTheme.colors
    val haptics = LocalHapticFeedback.current
    var dragged by remember { mutableStateOf<Long?>(null) }
    // How far the dragged song is from its place in the list, in pixels.
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val heights = remember { mutableStateMapOf<Long, Int>() }

    /** Follows the finger: past half a row, the song swaps places with its neighbor. */
    fun drag(key: Long, deltaY: Float) {
        dragOffset += deltaY
        val height = heights[key]?.toFloat()?.takeIf { it > 0f } ?: return
        while (true) {
            val index = player.upNext.indexOfFirst { it.key == key }
            val offset = when {
                dragOffset > height / 2 -> 1
                dragOffset < -height / 2 -> -1
                else -> return
            }
            player.move(key, offset)
            if (player.upNext.indexOfFirst { it.key == key } == index) return
            dragOffset -= offset * height
        }
    }

    fun stopDrag() {
        dragged = null
        dragOffset = 0f
    }

    Column {
        player.upNext.forEach { entry ->
            key(entry.key) {
                val isDragged = dragged == entry.key
                val locked = entry.key == player.sentKey
                Box(
                    Modifier
                        .onSizeChanged { heights[entry.key] = it.height }
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragged) dragOffset else 0f
                            shadowElevation = if (isDragged) 8.dp.toPx() else 0f
                            shape = AppShapes.small
                            clip = isDragged
                        }
                        .then(if (isDragged) Modifier.background(colors.surface, AppShapes.small) else Modifier),
                ) {
                    TrackRow(
                        api,
                        entry.track,
                        playing = false,
                        onClick = { player.playNow(entry) },
                        trailing = {
                            // Switching mode swaps the buttons: the handle slides in from the right.
                            AnimatedContent(
                                reordering && !locked,
                                transitionSpec = {
                                    (slideInHorizontally { it / 2 } + fadeIn()) togetherWith (slideOutHorizontally { it / 2 } + fadeOut()) using
                                        SizeTransform(clip = false)
                                },
                            ) { editing ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (editing) {
                                        IconButton(onClick = { player.removeFromQueue(entry.key) }) {
                                            Icon(Lucide.X, contentDescription = stringResource(Res.string.queue_remove), tint = colors.textSecondary)
                                        }
                                        Box(
                                            Modifier.size(48.dp).pointerInput(entry.key) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = {
                                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        dragged = entry.key
                                                        dragOffset = 0f
                                                    },
                                                    onDragEnd = ::stopDrag,
                                                    onDragCancel = ::stopDrag,
                                                    onDrag = { change, amount ->
                                                        change.consume()
                                                        drag(entry.key, amount.y)
                                                    },
                                                )
                                            },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            // The handle wiggles once when it appears, to show it can be grabbed.
                                            val wiggle = remember { Animatable(0f) }
                                            LaunchedEffect(Unit) {
                                                delay(150)
                                                wiggle.animateTo(0f, keyframes {
                                                    durationMillis = 450
                                                    -14f at 75
                                                    12f at 175
                                                    -8f at 275
                                                    4f at 375
                                                })
                                            }
                                            val grab by animateFloatAsState(if (isDragged) 1.25f else 1f)
                                            Icon(
                                                Lucide.GripVertical,
                                                contentDescription = stringResource(Res.string.queue_drag),
                                                tint = if (isDragged) colors.accentText else colors.textPrimary,
                                                modifier = Modifier.graphicsLayer {
                                                    rotationZ = wiggle.value
                                                    scaleX = grab
                                                    scaleY = grab
                                                },
                                            )
                                        }
                                    } else {
                                        entry.proposer?.let {
                                            PersonAvatar(api, it.userId, it.name, it.avatarColor, it.avatarVersion, size = 28.dp)
                                        }
                                        TrackMoreButton(api, player, entry.track)
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
