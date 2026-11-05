package com.musicroom.app.profile

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.BadgeCheck
import com.composables.icons.lucide.Lucide
import com.musicroom.app.common.Reveal
import com.musicroom.app.common.UserAvatar
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.profile_verified
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.UserResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Top of the profile: the avatar pops in with a bounce while an accent ring spreads around it, then the
 * name, the email and the "verified" badge arrive one after the other.
 */
@Composable
fun ProfileHeader(api: MusicRoomApi, user: UserResponse) {
    val colors = AppTheme.colors
    val pop = remember { Animatable(0f) }
    val ring = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 260f)) }
        delay(120)
        ring.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(96.dp)
                    .graphicsLayer {
                        val scale = 1f + 0.45f * ring.value
                        scaleX = scale
                        scaleY = scale
                        alpha = (1f - ring.value) * 0.7f
                    }
                    .border(3.dp, colors.accent, CircleShape),
            )
            UserAvatar(
                api,
                user,
                size = 96.dp,
                modifier = Modifier.graphicsLayer {
                    val scale = 0.5f + 0.5f * pop.value
                    scaleX = scale
                    scaleY = scale
                    alpha = pop.value.coerceIn(0f, 1f)
                },
            )
        }
        Reveal(1) { Text(user.displayName, style = AppTheme.typography.title, color = colors.textPrimary) }
        Reveal(2) { Text(user.email, style = AppTheme.typography.body, color = colors.textSecondary) }
        if (user.emailVerified) {
            Reveal(4) {
                Row(
                    Modifier.background(colors.accentSubtle, AppShapes.pill)
                        .padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Lucide.BadgeCheck, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(16.dp))
                    Text(stringResource(Res.string.profile_verified), style = AppTheme.typography.bodySmall, color = colors.accentText)
                }
            }
        }
    }
}
