package com.musicroom.app.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Lucide
import com.musicroom.app.common.EmptyState
import com.musicroom.app.common.PersonAvatar
import com.musicroom.app.common.Reveal
import com.musicroom.app.common.SubScreenScaffold
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.friend_accept
import com.musicroom.app.generated.resources.friend_decline
import com.musicroom.app.generated.resources.notifications_empty_body
import com.musicroom.app.generated.resources.notifications_empty_title
import com.musicroom.app.generated.resources.notifications_title
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.components.AppCard
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.FriendshipStatus
import com.musicroom.shared.dto.NotificationType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * The latest notifications, newest first; unread ones have a dot until the page marks them read.
 * A friend request can be answered here; a playlist or event invitation opens it, the others the profile.
 */
@Composable
fun NotificationsScreen(
    api: MusicRoomApi,
    social: SocialController,
    onBack: () -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenEvent: (String) -> Unit,
) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<UiText?>(null) }
    var busy by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            social.refreshNotifications()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = UiText.of(e)
        }
    }
    // Seen: the bell number goes away (also for those arriving while the page shows).
    LaunchedEffect(social.counts.unreadNotifications) { social.markNotificationsRead() }

    fun answer(userId: String, accept: Boolean) {
        busy = userId
        error = null
        scope.launch {
            try {
                if (accept) social.addFriend(userId) else social.removeFriend(userId)
                social.refreshNotifications()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = UiText.of(e)
            }
            busy = null
        }
    }

    SubScreenScaffold(stringResource(Res.string.notifications_title), onBack) {
        error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }
        val items = social.notifications
        when {
            items == null -> if (error == null) {
                Text(stringResource(Res.string.common_loading), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            }
            items.isEmpty() -> Reveal(0) {
                EmptyState(Lucide.Bell, stringResource(Res.string.notifications_empty_title), stringResource(Res.string.notifications_empty_body))
            }
            else -> AppCard(Modifier.fillMaxWidth()) {
                items.forEach { item ->
                    val actor = item.actor
                    val text = notificationText(item)
                    val open = {
                        val playlistId = item.playlistId
                        val eventId = item.eventId
                        when {
                            item.type == NotificationType.EVENT_INVITE && eventId != null -> onOpenEvent(eventId)
                            item.type == NotificationType.PLAYLIST_INVITE && playlistId != null -> onOpenPlaylist(playlistId)
                            actor != null -> onOpenUser(actor.id)
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().pressable(onClick = open).padding(vertical = AppTheme.spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box {
                            if (actor != null) {
                                PersonAvatar(api, actor.id, actor.displayName, actor.avatarColor, actor.avatarVersion, size = 44.dp)
                            }
                            if (!item.read) {
                                Box(Modifier.align(Alignment.TopEnd).size(10.dp).background(colors.accent, CircleShape))
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                            Text(text, style = AppTheme.typography.body, color = colors.textPrimary)
                            Text(timeAgo(item.createdAt), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
                            // Still waiting for an answer: answer it here.
                            if (item.type == NotificationType.FRIEND_REQUEST && actor?.friendship == FriendshipStatus.REQUEST_RECEIVED) {
                                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                                    AppButton(
                                        stringResource(Res.string.friend_accept),
                                        { answer(actor.id, accept = true) },
                                        enabled = busy != actor.id,
                                    )
                                    AppButton(
                                        stringResource(Res.string.friend_decline),
                                        { answer(actor.id, accept = false) },
                                        enabled = busy != actor.id,
                                        variant = AppButtonVariant.Secondary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
