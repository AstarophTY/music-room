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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCircle
import com.musicroom.app.common.UserAvatar
import com.musicroom.app.common.Equalizer
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.app_name
import com.musicroom.app.generated.resources.messages_title
import com.musicroom.app.generated.resources.notifications_title
import com.musicroom.app.generated.resources.profile_open
import com.musicroom.app.social.BadgedIconButton
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.SocialCountsResponse
import com.musicroom.shared.dto.UserResponse
import org.jetbrains.compose.resources.stringResource

/**
 * Avatar on the left (opens the profile), app name, a small live equalizer, then the notifications bell and the
 * chats, each with the number of what waits.
 */
@Composable
fun MainTopBar(
    api: MusicRoomApi,
    user: UserResponse,
    counts: SocialCountsResponse,
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenMessages: () -> Unit,
) {
    val colors = AppTheme.colors
    val openProfile = stringResource(Res.string.profile_open)
    Row(
        Modifier.fillMaxWidth().statusBarsPadding()
            .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(
            api,
            user,
            Modifier.pressable(onClick = onOpenProfile).semantics { contentDescription = openProfile },
            size = 40.dp,
        )
        Text(
            stringResource(Res.string.app_name),
            modifier = Modifier.weight(1f),
            style = AppTheme.typography.heading,
            color = colors.textPrimary,
        )
        Equalizer(color = colors.accentText, height = 18.dp)
        Row {
            BadgedIconButton(Lucide.Bell, stringResource(Res.string.notifications_title), counts.unreadNotifications, onOpenNotifications)
            BadgedIconButton(Lucide.MessageCircle, stringResource(Res.string.messages_title), counts.unreadMessages, onOpenMessages)
        }
    }
}
