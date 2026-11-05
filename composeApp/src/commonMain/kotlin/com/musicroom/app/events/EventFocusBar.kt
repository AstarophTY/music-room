package com.musicroom.app.events

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.CircleAlert
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Radio
import com.composables.icons.lucide.X
import com.musicroom.app.common.Equalizer
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.event_focus_listening
import com.musicroom.app.generated.resources.event_focus_proposed
import com.musicroom.app.generated.resources.event_unfocus
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.dto.EventStatus
import org.jetbrains.compose.resources.stringResource

/**
 * Thin bar above the player while an event is focused: its name (an equalizer while its music plays), or for a
 * moment the song just proposed to it by a swipe (or why it was refused). Tapping it opens the event ([onOpen]);
 * the cross stops listening to it.
 */
@Composable
fun EventFocusBar(focus: EventFocus, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val event = focus.event ?: return
    val colors = AppTheme.colors
    Row(
        modifier.fillMaxWidth().background(colors.accentSubtle).pressable(onClick = { onOpen(event.id) })
            .padding(start = AppTheme.spacing.md, end = AppTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            focus.notice,
            contentKey = { it?.id },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.weight(1f),
        ) { notice ->
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                val problem = notice?.problem
                when {
                    notice == null && event.status == EventStatus.PLAYING -> Equalizer(color = colors.accentText, height = 12.dp)
                    notice == null -> Icon(Lucide.Radio, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(16.dp))
                    problem != null -> Icon(Lucide.CircleAlert, contentDescription = null, tint = colors.error, modifier = Modifier.size(16.dp))
                    else -> Icon(Lucide.CircleCheck, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(16.dp))
                }
                Text(
                    when {
                        notice == null -> stringResource(Res.string.event_focus_listening, event.name)
                        problem != null -> problem.resolve()
                        else -> stringResource(Res.string.event_focus_proposed, notice.track.name)
                    },
                    style = AppTheme.typography.label,
                    color = if (problem != null) colors.error else colors.accentText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = focus::unfocus) {
            Icon(Lucide.X, contentDescription = stringResource(Res.string.event_unfocus), tint = colors.accentText)
        }
    }
}
