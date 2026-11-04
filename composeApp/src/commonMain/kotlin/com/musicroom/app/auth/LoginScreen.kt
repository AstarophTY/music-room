package com.musicroom.app.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mail
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_please_wait
import com.musicroom.app.generated.resources.field_email
import com.musicroom.app.generated.resources.field_password
import com.musicroom.app.generated.resources.login_button
import com.musicroom.app.generated.resources.login_forgot
import com.musicroom.app.generated.resources.login_missing
import com.musicroom.app.generated.resources.login_no_account
import com.musicroom.app.generated.resources.login_server
import com.musicroom.app.generated.resources.login_subtitle
import com.musicroom.app.generated.resources.login_title
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.components.AppTextField
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.TwoFactorRequiredException
import com.musicroom.shared.dto.LoginRequest
import com.musicroom.shared.dto.TwoFactorMethod
import com.musicroom.shared.dto.UserResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoginScreen(
    api: MusicRoomApi,
    socialSignIn: SocialSignIn,
    email: String,
    onEmailChange: (String) -> Unit,
    initialMessage: UiText?,
    onAuthenticated: (UserResponse) -> Unit,
    onSignUp: () -> Unit,
    onForgotPassword: () -> Unit,
    onTwoFactorRequired: (String, TwoFactorMethod) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(initialMessage) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    fun submit() {
        if (email.isBlank() || password.isEmpty()) {
            error = UiText.of(Res.string.login_missing)
            return
        }
        error = null
        loading = true
        scope.launch {
            try {
                onAuthenticated(api.login(LoginRequest(email, password)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: TwoFactorRequiredException) {
                onTwoFactorRequired(e.challengeToken, e.method)
            } catch (e: Exception) {
                error = UiText.of(e)
            } finally {
                loading = false
            }
        }
    }

    AuthScaffold(title = stringResource(Res.string.login_title), subtitle = stringResource(Res.string.login_subtitle)) {
        AppTextField(
            email, onEmailChange, stringResource(Res.string.field_email), Modifier.fillMaxWidth(),
            enabled = !loading, keyboardType = KeyboardType.Email, leadingIcon = Lucide.Mail,
        )
        AppTextField(
            password, { password = it }, stringResource(Res.string.field_password), Modifier.fillMaxWidth(),
            enabled = !loading, isPassword = true, leadingIcon = Lucide.Lock,
        )
        error?.let { Text(it.resolve(), style = typography.bodySmall, color = colors.error) }
        AppButton(
            text = stringResource(if (loading) Res.string.common_please_wait else Res.string.login_button),
            onClick = { submit() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
        )
        AppButton(
            text = stringResource(Res.string.login_forgot),
            onClick = onForgotPassword,
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
            variant = AppButtonVariant.Ghost,
        )
        SocialSignInButtons(
            api = api,
            socialSignIn = socialSignIn,
            enabled = !loading,
            onBusyChange = { loading = it },
            onError = { error = it },
            onAuthenticated = onAuthenticated,
            onTwoFactorRequired = onTwoFactorRequired,
        )
        AppButton(
            text = stringResource(Res.string.login_no_account),
            onClick = onSignUp,
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
            variant = AppButtonVariant.Ghost,
        )
        Text(stringResource(Res.string.login_server, api.baseUrl), style = typography.bodySmall, color = colors.textDisabled)
    }
}
