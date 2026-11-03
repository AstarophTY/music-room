package com.musicroom.app.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ImagePlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.User
import com.musicroom.app.common.StatusMessage
import com.musicroom.app.common.SubScreenScaffold
import com.musicroom.app.common.UiText
import com.musicroom.app.common.UserAvatar
import com.musicroom.app.common.rememberImagePicker
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.avatar_choose
import com.musicroom.app.generated.resources.avatar_color
import com.musicroom.app.generated.resources.avatar_remove
import com.musicroom.app.generated.resources.avatar_title
import com.musicroom.app.generated.resources.avatar_updated
import com.musicroom.app.generated.resources.common_please_wait
import com.musicroom.app.generated.resources.common_save
import com.musicroom.app.generated.resources.field_display_name
import com.musicroom.app.generated.resources.profile_edit
import com.musicroom.app.generated.resources.profile_saved
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.components.AppCard
import com.musicroom.app.theme.components.AppTextField
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.AuthValidation
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.UpdateProfileRequest
import com.musicroom.shared.dto.UserResponse
import io.ktor.http.ContentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Display name, profile photo and avatar color. */
@Composable
fun EditProfileScreen(api: MusicRoomApi, user: UserResponse, onUserChange: (UserResponse) -> Unit, onBack: () -> Unit) {
    var displayName by remember(user.displayName) { mutableStateOf(user.displayName) }
    var status by remember { mutableStateOf<StatusMessage?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val colors = AppTheme.colors

    fun run(success: UiText, action: suspend () -> UserResponse) {
        busy = true
        status = null
        scope.launch {
            try {
                onUserChange(action())
                status = StatusMessage(success, isError = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                status = StatusMessage(UiText.of(e), isError = true)
            } finally {
                busy = false
            }
        }
    }

    val pickImage = rememberImagePicker { picked ->
        if (picked != null) {
            run(UiText.of(Res.string.avatar_updated)) { api.uploadAvatar(picked.bytes, ContentType.parse(picked.mimeType)) }
        }
    }

    SubScreenScaffold(title = stringResource(Res.string.profile_edit), onBack = onBack) {
        AppCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            ) {
                Text(stringResource(Res.string.avatar_title), style = AppTheme.typography.heading, color = colors.textPrimary)
                UserAvatar(api, user, size = 112.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                    if (pickImage != null) {
                        AppButton(stringResource(Res.string.avatar_choose), pickImage, enabled = !busy, icon = Lucide.ImagePlus)
                    }
                    if (user.avatarVersion != null) {
                        AppButton(
                            stringResource(Res.string.avatar_remove),
                            { run(UiText.of(Res.string.avatar_updated)) { api.deleteAvatar() } },
                            enabled = !busy,
                            variant = AppButtonVariant.Secondary,
                            icon = Lucide.Trash2,
                        )
                    }
                }
                Text(stringResource(Res.string.avatar_color), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
                AvatarColorPicker(
                    selected = user.avatarColor,
                    onSelect = { color ->
                        run(UiText.of(Res.string.avatar_updated)) { api.updateProfile(UpdateProfileRequest(avatarColor = color)) }
                    },
                    enabled = !busy,
                )
            }
        }

        AppTextField(
            displayName, { displayName = it }, stringResource(Res.string.field_display_name), Modifier.fillMaxWidth(),
            enabled = !busy, leadingIcon = Lucide.User,
        )
        status?.let { Text(it.text.resolve(), style = AppTheme.typography.bodySmall, color = if (it.isError) colors.error else colors.success) }
        AppButton(
            text = stringResource(if (busy) Res.string.common_please_wait else Res.string.common_save),
            onClick = {
                val invalid = AuthValidation.displayNameError(displayName)
                if (invalid != null) {
                    status = StatusMessage(UiText.of(invalid), isError = true)
                } else {
                    run(UiText.of(Res.string.profile_saved)) { api.updateProfile(UpdateProfileRequest(displayName = displayName)) }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy && displayName.trim() != user.displayName,
        )
    }
}
