package com.musicroom.app.events

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.UserCheck
import com.composables.icons.lucide.UserPlus
import com.composables.icons.lucide.X
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.event_invite_none
import com.musicroom.app.generated.resources.event_invite_title
import com.musicroom.app.generated.resources.event_invited_remove
import com.musicroom.app.generated.resources.playlist_invite
import com.musicroom.app.social.PersonRow
import com.musicroom.app.theme.components.AppBottomSheet
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.PublicUserResponse
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.stringResource

/** The owner's friends, to invite to the event (they get a notification); those invited can be removed. */
@Composable
fun EventInviteSheet(api: MusicRoomApi, room: EventRoom, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    var friends by remember { mutableStateOf<List<PublicUserResponse>?>(null) }
    var error by remember { mutableStateOf<UiText?>(null) }
    LaunchedEffect(Unit) {
        friends = try {
            api.friends().friends
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = UiText.of(e)
            emptyList()
        }
    }
    val invited = room.detail?.invited.orEmpty().map { it.id }.toSet()

    AppBottomSheet(onDismiss) {
        Text(stringResource(Res.string.event_invite_title), style = AppTheme.typography.title, color = colors.textPrimary)
        error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }
        val list = friends
        when {
            list == null -> Text(stringResource(Res.string.common_loading), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            list.isEmpty() -> Text(stringResource(Res.string.event_invite_none), style = AppTheme.typography.body, color = colors.textSecondary)
            else -> list.forEach { friend ->
                PersonRow(api, friend, onClick = {}) {
                    if (friend.id in invited) {
                        AppButton(
                            stringResource(Res.string.event_invited_remove),
                            { room.uninvite(friend.id) },
                            variant = AppButtonVariant.Ghost,
                            icon = Lucide.X,
                        )
                        Icon(Lucide.UserCheck, contentDescription = null, tint = colors.accentText)
                    } else {
                        AppButton(
                            stringResource(Res.string.playlist_invite),
                            { room.invite(friend.id) },
                            variant = AppButtonVariant.Secondary,
                            icon = Lucide.UserPlus,
                        )
                    }
                }
            }
        }
    }
}
