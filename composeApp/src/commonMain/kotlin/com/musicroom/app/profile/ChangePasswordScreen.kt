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
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.musicroom.app.auth.PasswordFields
import com.musicroom.app.common.StatusMessage
import com.musicroom.app.common.SubScreenScaffold
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_please_wait
import com.musicroom.app.generated.resources.field_confirm_new_password
import com.musicroom.app.generated.resources.field_current_password
import com.musicroom.app.generated.resources.field_new_password
import com.musicroom.app.generated.resources.password_change_button
import com.musicroom.app.generated.resources.password_changed
import com.musicroom.app.generated.resources.profile_password
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppTextField
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.AuthValidation
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.ChangePasswordRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Changes the password. The server signs out the other devices and emails a security alert. */
@Composable
fun ChangePasswordScreen(api: MusicRoomApi, onBack: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<StatusMessage?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val colors = AppTheme.colors

    fun submit() {
        val invalid = AuthValidation.passwordError(password) ?: AuthValidation.passwordConfirmationError(password, confirmation)
        if (invalid != null) {
            status = StatusMessage(UiText.of(invalid), isError = true)
            return
        }
        busy = true
        scope.launch {
            try {
                api.changePassword(ChangePasswordRequest(current, password))
                current = ""
                password = ""
                confirmation = ""
                status = StatusMessage(UiText.of(Res.string.password_changed), isError = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                status = StatusMessage(UiText.of(e), isError = true)
            } finally {
                busy = false
            }
        }
    }

    SubScreenScaffold(title = stringResource(Res.string.profile_password), onBack = onBack) {
        AppTextField(
            current, { current = it }, stringResource(Res.string.field_current_password), Modifier.fillMaxWidth(),
            enabled = !busy, isPassword = true, leadingIcon = Lucide.Lock,
        )
        PasswordFields(
            password = password,
            onPasswordChange = { password = it },
            confirmation = confirmation,
            onConfirmationChange = { confirmation = it },
            passwordLabel = stringResource(Res.string.field_new_password),
            confirmationLabel = stringResource(Res.string.field_confirm_new_password),
            enabled = !busy,
        )
        status?.let { Text(it.text.resolve(), style = AppTheme.typography.bodySmall, color = if (it.isError) colors.error else colors.success) }
        AppButton(
            text = stringResource(if (busy) Res.string.common_please_wait else Res.string.password_change_button),
            onClick = { submit() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy && current.isNotEmpty() && password.isNotEmpty(),
        )
    }
}
