package com.musicroom.app.events

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowBigUp
import com.composables.icons.lucide.Lucide
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.event_vote
import com.musicroom.app.generated.resources.event_vote_remove
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Upvote of a song: an arrow over its number of votes, amber once the user voted. Tapping votes or takes the vote
 * back; the arrow bounces when the user votes and the number rolls when it changes.
 */
@Composable
fun VoteButton(votes: Int, voted: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val background by animateColorAsState(if (voted) colors.accent else colors.surface)
    val content by animateColorAsState(
        when {
            voted -> colors.onAccent
            enabled -> colors.textPrimary
            else -> colors.textDisabled
        },
    )
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(voted) {
        if (voted) {
            bounce.snapTo(0.7f)
            bounce.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 500f))
        }
    }
    val label = stringResource(if (voted) Res.string.event_vote_remove else Res.string.event_vote)
    Column(
        modifier.defaultMinSize(minWidth = 48.dp).pressable(enabled, onClick)
            .background(background, AppShapes.medium)
            .border(AppTheme.spacing.border, if (voted) colors.accent else colors.border, AppShapes.medium)
            .padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xs)
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Icon(
            Lucide.ArrowBigUp,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(20.dp).graphicsLayer {
                scaleX = bounce.value
                scaleY = bounce.value
            },
        )
        AnimatedContent(
            votes,
            transitionSpec = {
                val up = targetState > initialState
                (slideInVertically { if (up) it else -it } + fadeIn()) togetherWith (slideOutVertically { if (up) -it else it } + fadeOut())
            },
        ) { shown ->
            Text(shown.toString(), style = AppTheme.typography.label, color = content)
        }
    }
}
