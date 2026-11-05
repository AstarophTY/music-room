package com.musicroom.app.social

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.musicroom.app.common.PersonAvatar
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import kotlinx.coroutines.delay

/** Card that drops from the top when a notification arrives, and leaves after a few seconds. Tapping it opens [onOpen]. */
@Composable
fun NotificationBanner(api: MusicRoomApi, social: SocialController, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val current = social.latest
    // Keeps showing the last one while it slides away.
    var shown by remember { mutableStateOf(current) }
    if (current != null) shown = current
    LaunchedEffect(current?.id) {
        if (current == null) return@LaunchedEffect
        delay(4_500)
        social.dismissLatest()
    }

    AnimatedVisibility(
        visible = current != null,
        modifier = modifier,
        enter = slideInVertically(spring(dampingRatio = 0.7f, stiffness = 500f)) { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        val item = shown ?: return@AnimatedVisibility
        Row(
            Modifier.padding(AppTheme.spacing.md).widthIn(max = 480.dp).fillMaxWidth()
                .shadow(12.dp, AppShapes.large)
                .background(colors.surface, AppShapes.large)
                .border(AppTheme.spacing.border, colors.border, AppShapes.large)
                .clip(AppShapes.large)
                .pressable(onClick = {
                    social.dismissLatest()
                    onOpen()
                })
                .padding(AppTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item.actor?.let { PersonAvatar(api, it.id, it.displayName, it.avatarColor, it.avatarVersion, size = 36.dp) }
            Text(
                notificationText(item),
                style = AppTheme.typography.label,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
