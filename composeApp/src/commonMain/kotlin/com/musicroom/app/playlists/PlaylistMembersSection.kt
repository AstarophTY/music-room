package com.musicroom.app.playlists

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import com.composables.icons.lucide.LogOut
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mail
import com.composables.icons.lucide.UserPlus
import com.composables.icons.lucide.X
import com.musicroom.app.common.MemberAvatar
import com.musicroom.app.common.SectionTitle
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.playlist_access_read
import com.musicroom.app.generated.resources.playlist_access_write
import com.musicroom.app.generated.resources.playlist_invite
import com.musicroom.app.generated.resources.playlist_invite_email
import com.musicroom.app.generated.resources.playlist_leave
import com.musicroom.app.generated.resources.playlist_member_remove
import com.musicroom.app.generated.resources.playlist_members
import com.musicroom.app.generated.resources.playlist_owner
import com.musicroom.app.generated.resources.playlist_personal_hint
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.components.AppChip
import com.musicroom.app.theme.components.AppTextField
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.AuthValidation
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.PlaylistAccess
import com.musicroom.shared.dto.PlaylistDetailResponse
import com.musicroom.shared.dto.PlaylistMemberResponse
import com.musicroom.shared.dto.PlaylistType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Who is in the playlist and what they can do. The owner adds people by email with "can listen" or
 * "can edit" (only for a shared playlist), changes their rights and removes them; a member can leave.
 */
@Composable
fun PlaylistMembersSection(
    api: MusicRoomApi,
    detail: PlaylistDetailResponse,
    onChange: (PlaylistDetailResponse) -> Unit,
    onLeft: () -> Unit,
    myUserId: String,
) {
    val colors = AppTheme.colors
    val playlist = detail.playlist
    val shared = playlist.type == PlaylistType.SHARED
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var access by remember { mutableStateOf(PlaylistAccess.READ) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<UiText?>(null) }
    val inviteAccess = if (shared) access else PlaylistAccess.READ

    fun run(action: suspend () -> Unit) {
        busy = true
        error = null
        scope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = UiText.of(e)
            }
            busy = false
        }
    }

    @Composable
    fun accessLabel(value: PlaylistAccess) =
        stringResource(if (value == PlaylistAccess.WRITE) Res.string.playlist_access_write else Res.string.playlist_access_read)

    @Composable
    fun memberRow(member: PlaylistMemberResponse, label: String, actions: @Composable () -> Unit = {}) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MemberAvatar(api, member)
            Column(Modifier.weight(1f)) {
                Text(member.displayName, style = AppTheme.typography.label, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(label, style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            }
            actions()
        }
    }

    Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        SectionTitle(stringResource(Res.string.playlist_members))
        memberRow(playlist.owner, stringResource(Res.string.playlist_owner))
        detail.members.forEach { member ->
            memberRow(member, accessLabel(member.access)) {
                if (playlist.isOwner) {
                    if (shared) {
                        // Tapping the chip switches between "can listen" and "can edit".
                        AppChip(
                            accessLabel(member.access),
                            selected = member.access == PlaylistAccess.WRITE,
                            onClick = {
                                val next = if (member.access == PlaylistAccess.WRITE) PlaylistAccess.READ else PlaylistAccess.WRITE
                                if (!busy) run { onChange(api.updatePlaylistMember(playlist.id, member.userId, next)) }
                            },
                        )
                    }
                    IconButton(
                        onClick = {
                            run {
                                api.removePlaylistMember(playlist.id, member.userId)
                                onChange(api.roomPlaylist(playlist.id))
                            }
                        },
                        enabled = !busy,
                    ) {
                        Icon(Lucide.X, contentDescription = stringResource(Res.string.playlist_member_remove, member.displayName), tint = colors.textSecondary)
                    }
                }
            }
        }

        if (playlist.isOwner) {
            AppTextField(
                email,
                { email = it },
                stringResource(Res.string.playlist_invite_email),
                Modifier.fillMaxWidth(),
                enabled = !busy,
                keyboardType = KeyboardType.Email,
                leadingIcon = Lucide.Mail,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                AppChip(accessLabel(PlaylistAccess.READ), selected = inviteAccess == PlaylistAccess.READ, onClick = { access = PlaylistAccess.READ })
                if (shared) {
                    AppChip(accessLabel(PlaylistAccess.WRITE), selected = inviteAccess == PlaylistAccess.WRITE, onClick = { access = PlaylistAccess.WRITE })
                }
                Row(Modifier.weight(1f)) {}
                AppButton(
                    stringResource(Res.string.playlist_invite),
                    {
                        val invalid = AuthValidation.emailError(email.trim())
                        if (invalid != null) {
                            error = UiText.of(invalid)
                        } else {
                            run {
                                onChange(api.addPlaylistMember(playlist.id, email.trim(), inviteAccess))
                                email = ""
                            }
                        }
                    },
                    enabled = !busy && email.isNotBlank(),
                    icon = Lucide.UserPlus,
                )
            }
            if (!shared) {
                Text(stringResource(Res.string.playlist_personal_hint), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            }
        } else {
            AppButton(
                stringResource(Res.string.playlist_leave),
                {
                    run {
                        api.removePlaylistMember(playlist.id, myUserId)
                        onLeft()
                    }
                },
                enabled = !busy,
                variant = AppButtonVariant.Secondary,
                icon = Lucide.LogOut,
            )
        }
        error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }
    }
}
