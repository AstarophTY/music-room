package com.musicroom.app.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.composables.icons.lucide.ShieldCheck
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_please_wait
import com.musicroom.app.generated.resources.twofa_field_code
import com.musicroom.app.generated.resources.twofa_field_recovery
import com.musicroom.app.generated.resources.twofa_subtitle
import com.musicroom.app.generated.resources.twofa_title
import com.musicroom.app.generated.resources.twofa_use_app
import com.musicroom.app.generated.resources.twofa_use_recovery
import com.musicroom.app.generated.resources.twofa_verify
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.components.AppTextField
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.TwoFactorLoginRequest
import com.musicroom.shared.dto.UserResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Second step of a login on an account with 2FA: a code from the authenticator app, or a recovery code. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TwoFactorLoginScreen(
    api: MusicRoomApi,
    challengeToken: String,
    onBack: () -> Unit,
    onAuthenticated: (UserResponse) -> Unit,
) {
    var useRecovery by rememberSaveable { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<UiText?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = !loading) { onBack() }

    fun submit() {
        if (code.isBlank()) return
        error = null
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

    AuthScaffold(
        title = stringResource(Res.string.twofa_title),
        subtitle = stringResource(Res.string.twofa_subtitle),
        onBack = onBack,
        backEnabled = !loading,
    ) {
        if (useRecovery) {
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
                leadingIcon = Lucide.ShieldCheck,
            )
        }
        error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = AppTheme.colors.error) }
        AppButton(
            text = stringResource(if (loading) Res.string.common_please_wait else Res.string.twofa_verify),
            onClick = { submit() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading && code.isNotBlank(),
        )
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
