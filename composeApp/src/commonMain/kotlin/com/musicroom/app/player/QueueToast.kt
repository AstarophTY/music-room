package com.musicroom.app.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.ListPlus
import com.composables.icons.lucide.Lucide
import com.musicroom.app.common.PersonAvatar
import com.musicroom.app.common.RemoteImage
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.queue_added
import com.musicroom.app.generated.resources.queue_added_many
import com.musicroom.app.generated.resources.queue_proposed_by
import com.musicroom.app.generated.resources.queue_show
import com.musicroom.app.generated.resources.queue_undo
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import org.jetbrains.compose.resources.stringResource

/**
 * Floating card confirming songs added to the queue: the cover with a queue badge, what was added, the
 * profile picture of who proposed it (when it is someone else), Undo while the songs can be taken back, and
 * a button to see the queue ([onOpen], hidden when null). It rises from the bottom and leaves on its own.
 */
@Composable
fun QueueToast(api: MusicRoomApi, player: PlayerController, onOpen: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val current = player.queueNotice
    // Keeps showing the last confirmation while it slides away.
    var shown by remember { mutableStateOf(current) }
    if (current != null) shown = current

    AnimatedVisibility(
        visible = current != null,
        modifier = modifier,
        enter = slideInVertically(spring(dampingRatio = 0.7f, stiffness = 500f)) { it } + fadeIn() + scaleIn(initialScale = 0.9f),
        exit = slideOutVertically { it } + fadeOut(),
    ) {
        val notice = shown ?: return@AnimatedVisibility
        Row(
            Modifier.padding(AppTheme.spacing.md).widthIn(max = 480.dp).fillMaxWidth()
                .shadow(12.dp, AppShapes.large)
                .background(colors.surface, AppShapes.large)
                .border(AppTheme.spacing.border, colors.border, AppShapes.large)
                .clip(AppShapes.large)
                .then(if (onOpen != null) Modifier.pressable(onClick = onOpen) else Modifier)
                .padding(start = AppTheme.spacing.sm, end = AppTheme.spacing.xs, top = AppTheme.spacing.sm, bottom = AppTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // A new confirmation replaces the previous one with a small bounce.
            AnimatedContent(
                notice,
                contentKey = { it.id },
                transitionSpec = { (scaleIn(initialScale = 0.8f) + fadeIn()) togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
            ) { item ->
                val first = item.entries.first()
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        RemoteImage(api, first.track.imageUrl, Modifier.size(44.dp).clip(AppShapes.small))
                        Box(
                            Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp).size(20.dp)
                                .background(colors.accent, CircleShape).border(2.dp, colors.surface, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Lucide.ListPlus, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(12.dp))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        val count = item.entries.size
                        Text(
                            if (count == 1) stringResource(Res.string.queue_added) else stringResource(Res.string.queue_added_many, count),
                            style = AppTheme.typography.label,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val proposer = first.proposer?.takeIf { it.userId != player.me?.userId }
                        Text(
                            when {
                                count > 1 -> item.entries.joinToString(", ") { it.track.name }
                                proposer != null -> stringResource(Res.string.queue_proposed_by, first.track.name, proposer.name)
                                else -> listOf(first.track.name, first.track.artists.joinToString(", ")).joinToString(" · ")
                            },
                            style = AppTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    first.proposer?.takeIf { item.entries.size == 1 && it.userId != player.me?.userId }?.let {
                        PersonAvatar(api, it.userId, it.name, it.avatarColor, it.avatarVersion, size = 28.dp)
                    }
                }
            }
            if (player.canUndo(notice)) {
                AppButton(stringResource(Res.string.queue_undo), { player.undo(notice) }, variant = AppButtonVariant.Ghost)
            }
            if (onOpen != null) {
                IconButton(onClick = {
                    player.dismissNotice()
                    onOpen()
                }) {
                    Icon(Lucide.ListMusic, contentDescription = stringResource(Res.string.queue_show), tint = colors.textPrimary)
                }
            }
        }
    }
}
