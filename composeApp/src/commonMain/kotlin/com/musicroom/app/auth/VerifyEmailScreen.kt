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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.LogOut
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.musicroom.app.common.StatusMessage
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_please_wait
import com.musicroom.app.generated.resources.logout
import com.musicroom.app.generated.resources.verify_button
import com.musicroom.app.generated.resources.verify_help
import com.musicroom.app.generated.resources.verify_resend
import com.musicroom.app.generated.resources.verify_resend_wait
import com.musicroom.app.generated.resources.verify_resent
import com.musicroom.app.generated.resources.verify_subtitle
import com.musicroom.app.generated.resources.verify_title
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.AuthValidation
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.UserResponse
import com.musicroom.shared.dto.VerifyEmailRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Shown while the account's email is not verified: the user types the 6-digit code received by email. */
@Composable
fun VerifyEmailScreen(
    api: MusicRoomApi,
    user: UserResponse,
    onVerified: (UserResponse) -> Unit,
    onLoggedOut: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<StatusMessage?>(null) }
    var busy by remember { mutableStateOf(false) }
    // Seconds before a new code can be asked (the server allows one email per minute).
    var resendWait by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    LaunchedEffect(resendWait > 0) {
        while (resendWait > 0) {
            delay(1_000)
            resendWait--
        }
    }

    fun run(action: suspend () -> Unit) {
        busy = true
        scope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                status = StatusMessage(UiText.of(e), isError = true)
            } finally {
                busy = false
            }
        }
    }

    fun verify() {
        val invalid = AuthValidation.verificationCodeError(code)
        if (invalid != null) {
            status = StatusMessage(UiText.of(invalid), isError = true)
            return
        }
        status = null
        run { onVerified(api.verifyEmail(VerifyEmailRequest(code))) }
    }

    AuthScaffold(
        title = stringResource(Res.string.verify_title),
        subtitle = stringResource(Res.string.verify_subtitle, AuthValidation.VERIFICATION_CODE_LENGTH, user.email),
    ) {
        CodeField(
            code = code,
            onCodeChange = {
                code = it
                if (code.length == AuthValidation.VERIFICATION_CODE_LENGTH && !busy) verify()
            },
            enabled = !busy,
        )
        status?.let { Text(it.text.resolve(), style = typography.bodySmall, color = if (it.isError) colors.error else colors.success) }
        AppButton(
            text = stringResource(if (busy) Res.string.common_please_wait else Res.string.verify_button),
            onClick = { verify() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
        )
        AppButton(
            text = if (resendWait > 0) {
                stringResource(Res.string.verify_resend_wait, resendWait)
            } else {
                stringResource(Res.string.verify_resend)
            },
            onClick = {
                status = null
                run {
                    api.resendVerificationEmail()
                    code = ""
                    resendWait = 60
                    status = StatusMessage(UiText.of(Res.string.verify_resent, user.email), isError = false)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy && resendWait == 0,
            variant = AppButtonVariant.Secondary,
            icon = Lucide.RefreshCw,
        )
        Text(stringResource(Res.string.verify_help), style = typography.bodySmall, color = colors.textSecondary)
        AppButton(
            text = stringResource(Res.string.logout),
            onClick = {
                busy = true
                scope.launch {
                    api.logout()
                    onLoggedOut()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
            variant = AppButtonVariant.Ghost,
            icon = Lucide.LogOut,
        )
    }
}
