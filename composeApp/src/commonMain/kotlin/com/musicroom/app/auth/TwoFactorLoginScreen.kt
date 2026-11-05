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
import androidx.compose.ui.text.input.KeyboardType
import com.composables.icons.lucide.KeyRound
import com.composables.icons.lucide.LifeBuoy
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mail
import com.composables.icons.lucide.ShieldCheck
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_please_wait
import com.musicroom.app.generated.resources.twofa_email_sent
import com.musicroom.app.generated.resources.twofa_email_subtitle
import com.musicroom.app.generated.resources.twofa_field_code
import com.musicroom.app.generated.resources.twofa_field_recovery
import com.musicroom.app.generated.resources.twofa_subtitle
import com.musicroom.app.generated.resources.twofa_title
import com.musicroom.app.generated.resources.twofa_use_app
import com.musicroom.app.generated.resources.twofa_use_recovery
import com.musicroom.app.generated.resources.twofa_verify
import com.musicroom.app.generated.resources.verify_resend
import com.musicroom.app.generated.resources.verify_resend_wait
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.components.AppTextField
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.TwoFactorLoginRequest
import com.musicroom.shared.dto.TwoFactorMethod
import com.musicroom.shared.dto.TwoFactorResendRequest
import com.musicroom.shared.dto.UserResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Second step of a login on an account with 2FA: a code from the authenticator app or a recovery code,
 * or, with [TwoFactorMethod.EMAIL], the code the server just emailed.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TwoFactorLoginScreen(
    api: MusicRoomApi,
    challengeToken: String,
    method: TwoFactorMethod,
    onBack: () -> Unit,
    onAuthenticated: (UserResponse) -> Unit,
) {
    val byEmail = method == TwoFactorMethod.EMAIL
    val resendSeconds = 60
    var useRecovery by rememberSaveable { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<UiText?>(null) }
    var info by remember { mutableStateOf<UiText?>(null) }
    var loading by remember { mutableStateOf(false) }
    // The server sent a code with the challenge: a new one can be asked after a minute.
    var resendWait by remember { mutableIntStateOf(if (byEmail) resendSeconds else 0) }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = !loading) { onBack() }

    LaunchedEffect(resendWait > 0) {
        while (resendWait > 0) {
            delay(1_000)
            resendWait--
        }
    }

    fun submit() {
        if (code.isBlank()) return
        error = null
        info = null
        loading = true
        scope.launch {
            try {
                onAuthenticated(api.verifyTwoFactor(TwoFactorLoginRequest(challengeToken, code)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = UiText.of(e)
                code = ""
            } finally {
                loading = false
            }
        }
    }

    fun resend() {
        error = null
        info = null
        loading = true
        scope.launch {
            try {
                api.resendTwoFactorEmail(TwoFactorResendRequest(challengeToken))
                info = UiText.of(Res.string.twofa_email_sent)
                resendWait = resendSeconds
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = UiText.of(e)
            } finally {
                loading = false
            }
        }
    }

    AuthScaffold(
        title = stringResource(Res.string.twofa_title),
        subtitle = stringResource(if (byEmail) Res.string.twofa_email_subtitle else Res.string.twofa_subtitle),
        onBack = onBack,
        backEnabled = !loading,
    ) {
        if (useRecovery && !byEmail) {
            AppTextField(
                code, { code = it.uppercase().take(11) }, stringResource(Res.string.twofa_field_recovery), Modifier.fillMaxWidth(),
                enabled = !loading, leadingIcon = Lucide.LifeBuoy,
            )
        } else {
            AppTextField(
                value = code,
                onValueChange = { input ->
                    code = input.filter { it.isDigit() }.take(6)
                    if (code.length == 6 && !loading) submit()
                },
                label = stringResource(Res.string.twofa_field_code),
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                keyboardType = KeyboardType.NumberPassword,
                leadingIcon = if (byEmail) Lucide.Mail else Lucide.ShieldCheck,
            )
        }
        error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = AppTheme.colors.error) }
        info?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = AppTheme.colors.success) }
        AppButton(
            text = stringResource(if (loading) Res.string.common_please_wait else Res.string.twofa_verify),
            onClick = { submit() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading && code.isNotBlank(),
        )
        if (byEmail) {
            AppButton(
                text = if (resendWait > 0) {
                    stringResource(Res.string.verify_resend_wait, resendWait)
                } else {
                    stringResource(Res.string.verify_resend)
                },
                onClick = { resend() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading && resendWait == 0,
                variant = AppButtonVariant.Ghost,
                icon = Lucide.Mail,
            )
        } else {
            AppButton(
                text = stringResource(if (useRecovery) Res.string.twofa_use_app else Res.string.twofa_use_recovery),
                onClick = {
                    useRecovery = !useRecovery
                    code = ""
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                variant = AppButtonVariant.Ghost,
                icon = if (useRecovery) Lucide.ShieldCheck else Lucide.KeyRound,
            )
        }
    }
}
