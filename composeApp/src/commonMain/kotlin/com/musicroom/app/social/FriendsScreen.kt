package com.musicroom.app.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCircle
import com.composables.icons.lucide.Users
import com.composables.icons.lucide.X
import com.musicroom.app.common.EmptyState
import com.musicroom.app.common.Reveal
import com.musicroom.app.common.SubScreenScaffold
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.friend_accept
import com.musicroom.app.generated.resources.friend_cancel_request
import com.musicroom.app.generated.resources.friend_decline
import com.musicroom.app.generated.resources.friends_empty_body
import com.musicroom.app.generated.resources.friends_empty_title
import com.musicroom.app.generated.resources.friends_find
import com.musicroom.app.generated.resources.friends_list
import com.musicroom.app.generated.resources.friends_requests
import com.musicroom.app.generated.resources.friends_sent
import com.musicroom.app.generated.resources.friends_title
import com.musicroom.app.generated.resources.messages_open
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.components.AppCard
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * The user's friends (each with a button to chat), the requests they received (accept or decline) and the ones
 * they sent (to take back). [onFindPeople] goes to the search, where people are found by name.
 */
@Composable
fun FriendsScreen(
    api: MusicRoomApi,
    social: SocialController,
    onBack: () -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    onFindPeople: () -> Unit,
) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<UiText?>(null) }
    var busy by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            social.refreshFriends()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = UiText.of(e)
        }
    }

    fun act(userId: String, accept: Boolean) {
        busy = userId
        error = null
        scope.launch {
            try {
                if (accept) social.addFriend(userId) else social.removeFriend(userId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = UiText.of(e)
            }
            busy = null
        }
    }

    val label: @Composable (String) -> Unit = { text ->
        Text(text, style = AppTheme.typography.label, color = colors.textSecondary)
    }

    SubScreenScaffold(stringResource(Res.string.friends_title), onBack) {
        error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }
        val lists = social.friends
        when {
            lists == null -> if (error == null) {
                Text(stringResource(Res.string.common_loading), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            }
            lists.friends.isEmpty() && lists.received.isEmpty() && lists.sent.isEmpty() -> Reveal(0) {
                EmptyState(
                    Lucide.Users,
                    stringResource(Res.string.friends_empty_title),
                    stringResource(Res.string.friends_empty_body),
                    actionLabel = stringResource(Res.string.friends_find),
                    onAction = onFindPeople,
                )
            }
            else -> {
                if (lists.received.isNotEmpty()) {
                    Reveal(0) {
                        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                            label(stringResource(Res.string.friends_requests))
                            AppCard(Modifier.fillMaxWidth()) {
                                lists.received.forEach { user ->
                                    PersonRow(api, user, onClick = { onOpenUser(user.id) }) {
                                        IconButton(onClick = { act(user.id, accept = false) }, enabled = busy != user.id) {
                                            Icon(Lucide.X, stringResource(Res.string.friend_decline), tint = colors.textSecondary)
                                        }
                                        IconButton(onClick = { act(user.id, accept = true) }, enabled = busy != user.id) {
                                            Icon(
                                                Lucide.Check,
                                                stringResource(Res.string.friend_accept),
                                                tint = colors.onAccent,
                                                modifier = Modifier.background(colors.accent, CircleShape).size(32.dp).padding(6.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Reveal(1) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                        label(stringResource(Res.string.friends_list) + " · " + lists.friends.size)
                        if (lists.friends.isEmpty()) {
                            AppButton(
                                stringResource(Res.string.friends_find),
                                onFindPeople,
                                Modifier.fillMaxWidth(),
                                variant = AppButtonVariant.Secondary,
                                icon = Lucide.Users,
                            )
                        } else {
                            AppCard(Modifier.fillMaxWidth()) {
                                lists.friends.forEach { user ->
                                    PersonRow(api, user, onClick = { onOpenUser(user.id) }) {
                                        IconButton(onClick = { onOpenChat(user.id) }) {
                                            Icon(Lucide.MessageCircle, stringResource(Res.string.messages_open), tint = colors.accentText)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (lists.sent.isNotEmpty()) {
                    Reveal(2) {
                        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                            label(stringResource(Res.string.friends_sent))
                            AppCard(Modifier.fillMaxWidth()) {
                                lists.sent.forEach { user ->
                                    PersonRow(api, user, onClick = { onOpenUser(user.id) }) {
                                        AppButton(
                                            stringResource(Res.string.friend_cancel_request),
                                            { act(user.id, accept = false) },
                                            enabled = busy != user.id,
                                            variant = AppButtonVariant.Ghost,
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
}
