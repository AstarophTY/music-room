package com.musicroom.app.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCircle
import com.composables.icons.lucide.Users
import com.musicroom.app.common.EmptyState
import com.musicroom.app.common.Reveal
import com.musicroom.app.common.SubScreenScaffold
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.chat_say_hi
import com.musicroom.app.generated.resources.chat_typing
import com.musicroom.app.generated.resources.chat_you
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.friends_find
import com.musicroom.app.generated.resources.friends_title
import com.musicroom.app.generated.resources.messages_empty_body
import com.musicroom.app.generated.resources.messages_empty_title
import com.musicroom.app.generated.resources.messages_title
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.components.AppCard
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.stringResource

/**
 * Chats with each friend, the latest first: their last message (bold while unread), when it was sent and how many
 * are unread. "typing…" replaces the last message while the friend writes.
 */
@Composable
fun ConversationsScreen(
    api: MusicRoomApi,
    social: SocialController,
    meId: String,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenFriends: () -> Unit,
    onFindPeople: () -> Unit,
) {
    val colors = AppTheme.colors
    var error by remember { mutableStateOf<UiText?>(null) }

    LaunchedEffect(Unit) {
        try {
            social.refreshConversations()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = UiText.of(e)
        }
    }

    SubScreenScaffold(stringResource(Res.string.messages_title), onBack) {
        AppButton(
            stringResource(Res.string.friends_title),
            onOpenFriends,
            Modifier.fillMaxWidth(),
            variant = AppButtonVariant.Secondary,
            icon = Lucide.Users,
        )
        error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }
        val items = social.conversations
        when {
            items == null -> if (error == null) {
                Text(stringResource(Res.string.common_loading), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            }
            items.isEmpty() -> Reveal(0) {
                EmptyState(
                    Lucide.MessageCircle,
                    stringResource(Res.string.messages_empty_title),
                    stringResource(Res.string.messages_empty_body),
                    actionLabel = stringResource(Res.string.friends_find),
                    onAction = onFindPeople,
                )
            }
            else -> AppCard(Modifier.fillMaxWidth()) {
                items.forEach { conversation ->
                    val friend = conversation.friend
                    val last = conversation.lastMessage
                    val unread = conversation.unreadCount > 0
                    val subtitle = when {
                        social.typing[friend.id] == true -> stringResource(Res.string.chat_typing)
                        last == null -> stringResource(Res.string.chat_say_hi, friend.displayName)
                        last.senderId == meId -> stringResource(Res.string.chat_you, last.body)
                        else -> last.body
                    }
                    PersonRow(
                        api,
                        friend,
                        onClick = { onOpenChat(friend.id) },
                        subtitle = subtitle,
                        subtitleBold = unread,
                    ) {
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                            if (last != null) {
                                Text(
                                    timeAgo(last.sentAt),
                                    style = AppTheme.typography.bodySmall,
                                    color = if (unread) colors.accentText else colors.textSecondary,
                                )
                            }
                            CountBadge(conversation.unreadCount)
                        }
                    }
                }
            }
        }
    }
}
