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
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.ExperimentalComposeUiApi
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_continue
import com.musicroom.app.generated.resources.common_please_wait
import com.musicroom.app.generated.resources.field_confirm_password
import com.musicroom.app.generated.resources.field_password
import com.musicroom.app.generated.resources.signup_create
import com.musicroom.app.generated.resources.signup_have_account
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.ApiException
import com.musicroom.shared.AuthValidation
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.RegisterRequest
import com.musicroom.shared.dto.UserResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Sign-up in three pages: email, password (with confirmation), then display name and terms. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SignUpScreen(
    api: MusicRoomApi,
    socialSignIn: SocialSignIn,
    email: String,
    onEmailChange: (String) -> Unit,
    onAuthenticated: (UserResponse) -> Unit,
    onLogIn: () -> Unit,
) {
    var step by rememberSaveable { mutableStateOf(SignUpStep.Email) }
    var displayName by rememberSaveable { mutableStateOf("") }
    var acceptedTerms by rememberSaveable { mutableStateOf(false) }
    // Passwords are not saved in the instance state, like on the login screen.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var showTerms by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<UiText?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val steps = SignUpStep.entries

    fun goTo(target: SignUpStep) {
        error = null
        step = target
    }

    fun back() = if (step == SignUpStep.Email) onLogIn() else goTo(steps[step.ordinal - 1])

    BackHandler(enabled = !loading) { back() }

    fun register() {
        loading = true
        scope.launch {
            try {
                onAuthenticated(api.register(RegisterRequest(email, password, displayName, acceptedTerms)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Email already used: send the user back to the page where they can change it.
                if (e is ApiException && e.error == ApiErrors.EMAIL_TAKEN) step = SignUpStep.Email
                error = UiText.of(e)
            } finally {
                loading = false
            }
        }
    }

    /** Checks the current page, then moves to the next one or creates the account on the last page. */
    fun next() {
        val invalid = when (step) {
            SignUpStep.Email -> AuthValidation.emailError(AuthValidation.normalizeEmail(email))
            SignUpStep.Password -> AuthValidation.passwordError(password)
                ?: AuthValidation.passwordConfirmationError(password, confirmation)
            SignUpStep.Profile -> AuthValidation.displayNameError(displayName)
                ?: AuthValidation.termsError(acceptedTerms)
        }
        error = invalid?.let { UiText.of(it) }
        if (invalid != null) return
        when (step) {
            SignUpStep.Email -> goTo(SignUpStep.Password)
            SignUpStep.Password -> goTo(SignUpStep.Profile)
            SignUpStep.Profile -> register()
        }
    }

    AuthScaffold(
        title = stringResource(step.title),
        subtitle = stringResource(step.subtitle),
        onBack = { back() },
        backEnabled = !loading,
        step = step.ordinal + 1,
        stepCount = steps.size,
    ) {
        when (step) {
            SignUpStep.Email -> SignUpEmailStep(email, onEmailChange, enabled = !loading)
            SignUpStep.Password -> PasswordFields(
                password = password,
                onPasswordChange = { password = it },
                confirmation = confirmation,
                onConfirmationChange = { confirmation = it },
                passwordLabel = stringResource(Res.string.field_password),
                confirmationLabel = stringResource(Res.string.field_confirm_password),
                enabled = !loading,
            )
            SignUpStep.Profile -> SignUpProfileStep(
                displayName = displayName,
                onDisplayNameChange = { displayName = it },
                acceptedTerms = acceptedTerms,
                onAcceptedTermsChange = { acceptedTerms = it },
                onShowTerms = { showTerms = true },
                enabled = !loading,
            )
        }

        error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = AppTheme.colors.error) }

        AppButton(
            text = stringResource(
                when {
                    loading -> Res.string.common_please_wait
                    step == SignUpStep.Profile -> Res.string.signup_create
                    else -> Res.string.common_continue
                },
            ),
            onClick = { next() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
        )

        if (step == SignUpStep.Email) {
            SocialSignInButtons(
                api = api,
                socialSignIn = socialSignIn,
                enabled = !loading,
                onBusyChange = { loading = it },
                onError = { error = it },
                onAuthenticated = onAuthenticated,
            )
            AppButton(
                text = stringResource(Res.string.signup_have_account),
                onClick = onLogIn,
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                variant = AppButtonVariant.Ghost,
            )
        }
    }

    if (showTerms) {
        TermsDialog(
            onDismiss = { showTerms = false },
            onAccept = {
                acceptedTerms = true
                showTerms = false
            },
        )
    }
}
