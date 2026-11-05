package com.musicroom.app.social

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.UserCheck
import com.composables.icons.lucide.UserPlus
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_cancel
import com.musicroom.app.generated.resources.friend_accept
import com.musicroom.app.generated.resources.friend_add
import com.musicroom.app.generated.resources.friend_friends
import com.musicroom.app.generated.resources.friend_remove
import com.musicroom.app.generated.resources.friend_remove_confirm
import com.musicroom.app.generated.resources.friend_requested
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.dto.FriendshipStatus
import com.musicroom.shared.dto.PublicUserResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * The friendship action with [user]: add, take back the request, accept theirs, or (when friends) remove them
 * after a confirmation. [onChange] gets the new status, [onError] what went wrong.
 */
@Composable
fun FriendButton(
    social: SocialController,
    user: PublicUserResponse,
    onChange: (FriendshipStatus) -> Unit,
    onError: (UiText) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }

    fun act(remove: Boolean) {
        busy = true
        scope.launch {
            try {
                if (remove) {
                    social.removeFriend(user.id)
                    onChange(FriendshipStatus.NONE)
                } else {
                    onChange(social.addFriend(user.id).friendship)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(UiText.of(e))
            }
            busy = false
        }
    }

    when (user.friendship) {
        FriendshipStatus.NONE ->
            AppButton(stringResource(Res.string.friend_add), { act(remove = false) }, modifier, enabled = !busy, icon = Lucide.UserPlus)
        FriendshipStatus.REQUEST_SENT -> AppButton(
            stringResource(Res.string.friend_requested),
            { act(remove = true) },
            modifier,
            enabled = !busy,
            variant = AppButtonVariant.Secondary,
            icon = Lucide.Clock,
        )
        FriendshipStatus.REQUEST_RECEIVED ->
            AppButton(stringResource(Res.string.friend_accept), { act(remove = false) }, modifier, enabled = !busy, icon = Lucide.UserCheck)
        FriendshipStatus.FRIENDS -> AppButton(
            stringResource(Res.string.friend_friends),
            { confirmRemove = true },
            modifier,
            enabled = !busy,
            variant = AppButtonVariant.Secondary,
            icon = Lucide.UserCheck,
        )
        FriendshipStatus.SELF -> Unit
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            containerColor = colors.surface,
            shape = AppShapes.large,
            text = {
                Text(
                    stringResource(Res.string.friend_remove_confirm, user.displayName),
                    style = AppTheme.typography.body,
                    color = colors.textPrimary,
                )
            },
            confirmButton = {
                AppButton(
                    stringResource(Res.string.friend_remove),
                    {
                        confirmRemove = false
                        act(remove = true)
                    },
                )
            },
            dismissButton = {
                AppButton(stringResource(Res.string.common_cancel), { confirmRemove = false }, variant = AppButtonVariant.Ghost)
            },
        )
    }
}
