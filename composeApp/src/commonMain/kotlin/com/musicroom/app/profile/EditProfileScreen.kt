package com.musicroom.app.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.User
import com.musicroom.app.common.StatusMessage
import com.musicroom.app.common.SubScreenScaffold
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_please_wait
import com.musicroom.app.generated.resources.common_save
import com.musicroom.app.generated.resources.field_display_name
import com.musicroom.app.generated.resources.profile_edit
import com.musicroom.app.generated.resources.profile_saved
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppTextField
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.AuthValidation
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.UpdateProfileRequest
import com.musicroom.shared.dto.UserResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun EditProfileScreen(api: MusicRoomApi, user: UserResponse, onUserChange: (UserResponse) -> Unit, onBack: () -> Unit) {
    var displayName by remember(user.displayName) { mutableStateOf(user.displayName) }
    var status by remember { mutableStateOf<StatusMessage?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val colors = AppTheme.colors

    fun save() {
        val invalid = AuthValidation.displayNameError(displayName)
        if (invalid != null) {
            status = StatusMessage(UiText.of(invalid), isError = true)
            return
        }
        busy = true
        scope.launch {
            try {
                onUserChange(api.updateProfile(UpdateProfileRequest(displayName)))
                status = StatusMessage(UiText.of(Res.string.profile_saved), isError = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                status = StatusMessage(UiText.of(e), isError = true)
            } finally {
                busy = false
            }
        }
    }

    SubScreenScaffold(title = stringResource(Res.string.profile_edit), onBack = onBack) {
        AppTextField(
            displayName, { displayName = it }, stringResource(Res.string.field_display_name), Modifier.fillMaxWidth(),
            enabled = !busy, leadingIcon = Lucide.User,
        )
        status?.let { Text(it.text.resolve(), style = AppTheme.typography.bodySmall, color = if (it.isError) colors.error else colors.success) }
        AppButton(
            text = stringResource(if (busy) Res.string.common_please_wait else Res.string.common_save),
            onClick = { save() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy && displayName.trim() != user.displayName,
        )
    }
}
