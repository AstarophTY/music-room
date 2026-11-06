package com.musicroom.app.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCircle
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.home_greeting
import com.musicroom.app.generated.resources.home_playlists_title
import com.musicroom.app.generated.resources.messages_title
import com.musicroom.app.generated.resources.notifications_title
import com.musicroom.app.generated.resources.tab_events
import com.musicroom.app.generated.resources.tab_search
import com.musicroom.app.generated.resources.tab_settings
import com.musicroom.app.social.BadgedIconButton
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.dto.SocialCountsResponse
import com.musicroom.shared.dto.UserResponse
import org.jetbrains.compose.resources.stringResource

/**
 * Top bar: shows the current tab title (display typography) on the left aligned with the notifications bell
 * and messages chat icons on the right.
 */
@Composable
fun MainTopBar(
    tab: MainTab,
    user: UserResponse,
    counts: SocialCountsResponse,
    onOpenNotifications: () -> Unit,
    onOpenMessages: () -> Unit,
) {
    val colors = AppTheme.colors
    val title = when (tab) {
        MainTab.Home -> stringResource(Res.string.home_greeting, user.displayName)
        MainTab.Search -> stringResource(Res.string.tab_search)
        MainTab.Events -> stringResource(Res.string.tab_events)
        MainTab.Playlists -> stringResource(Res.string.home_playlists_title)
        MainTab.Settings -> stringResource(Res.string.tab_settings)
    }
    Row(
        Modifier.fillMaxWidth().statusBarsPadding()
            .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = AppTheme.typography.display,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            BadgedIconButton(Lucide.Bell, stringResource(Res.string.notifications_title), counts.unreadNotifications, onOpenNotifications)
            BadgedIconButton(Lucide.MessageCircle, stringResource(Res.string.messages_title), counts.unreadMessages, onOpenMessages)
        }
    }
}
