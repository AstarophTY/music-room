package com.musicroom.app.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_please_wait
import com.musicroom.app.generated.resources.field_confirm_new_password
import com.musicroom.app.generated.resources.field_new_password
import com.musicroom.app.generated.resources.forgot_send
import com.musicroom.app.generated.resources.forgot_subtitle
import com.musicroom.app.generated.resources.forgot_title
import com.musicroom.app.generated.resources.reset_button
import com.musicroom.app.generated.resources.reset_subtitle
import com.musicroom.app.generated.resources.reset_title
import com.musicroom.app.generated.resources.verify_resend
import com.musicroom.app.generated.resources.verify_resend_wait
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.AuthValidation
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.ForgotPasswordRequest
import com.musicroom.shared.dto.ResetPasswordRequest
import com.musicroom.shared.dto.UserResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Forgot password: email -> code by email -> new password. Ends logged in, other devices signed out. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ForgotPasswordScreen(
    api: MusicRoomApi,
    email: String,
    onEmailChange: (String) -> Unit,
    onBack: () -> Unit,
    onAuthenticated: (UserResponse) -> Unit,
) {
    var codeSent by rememberSaveable { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<UiText?>(null) }
    var loading by remember { mutableStateOf(false) }
    var resendWait by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val colors = AppTheme.colors

    LaunchedEffect(resendWait > 0) {
        while (resendWait > 0) {
            delay(1_000)
            resendWait--
        }
    }

    fun back() {
        error = null
        if (codeSent) codeSent = false else onBack()
    }

    BackHandler(enabled = !loading) { back() }

    fun run(action: suspend () -> Unit) {
        error = null
        loading = true
        scope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = UiText.of(e)
            } finally {
                loading = false
            }
        }
    }

    fun sendCode() {
        val invalid = AuthValidation.emailError(AuthValidation.normalizeEmail(email))
        if (invalid != null) {
            error = UiText.of(invalid)
            return
        }
        run {
            api.forgotPassword(ForgotPasswordRequest(email))
            codeSent = true
            resendWait = 60
        }
    }

    fun reset() {
        val invalid = AuthValidation.verificationCodeError(code)
            ?: AuthValidation.passwordError(password)
            ?: AuthValidation.passwordConfirmationError(password, confirmation)
        if (invalid != null) {
            error = UiText.of(invalid)
            return
        }
        run { onAuthenticated(api.resetPassword(ResetPasswordRequest(email, code, password))) }
    }

    AuthScaffold(
        title = stringResource(if (codeSent) Res.string.reset_title else Res.string.forgot_title),
        subtitle = if (codeSent) {
            stringResource(Res.string.reset_subtitle, AuthValidation.normalizeEmail(email))
        } else {
            stringResource(Res.string.forgot_subtitle)
        },
        onBack = { back() },
        backEnabled = !loading,
    ) {
        if (!codeSent) {
            SignUpEmailStep(email, onEmailChange, enabled = !loading)
        } else {
            CodeField(code, { code = it }, enabled = !loading)
            PasswordFields(
                password = password,
                onPasswordChange = { password = it },
                confirmation = confirmation,
                onConfirmationChange = { confirmation = it },
                passwordLabel = stringResource(Res.string.field_new_password),
                confirmationLabel = stringResource(Res.string.field_confirm_new_password),
                enabled = !loading,
            )
        }
        error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }
        AppButton(
            text = stringResource(
                when {
                    loading -> Res.string.common_please_wait
                    codeSent -> Res.string.reset_button
                    else -> Res.string.forgot_send
                },
            ),
            onClick = { if (codeSent) reset() else sendCode() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
        )
        if (codeSent) {
            AppButton(
                text = if (resendWait > 0) {
                    stringResource(Res.string.verify_resend_wait, resendWait)
                } else {
                    stringResource(Res.string.verify_resend)
                },
                onClick = { sendCode() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading && resendWait == 0,
                variant = AppButtonVariant.Secondary,
                icon = Lucide.RefreshCw,
            )
        }
    }
}
