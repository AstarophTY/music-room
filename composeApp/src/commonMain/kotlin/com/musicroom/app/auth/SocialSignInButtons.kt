package com.musicroom.app.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_or
import com.musicroom.app.generated.resources.social_continue
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.ApiException
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.TwoFactorRequiredException
import com.musicroom.shared.dto.OAuthLoginRequest
import com.musicroom.shared.dto.OAuthProvider
import com.musicroom.shared.dto.TwoFactorMethod
import com.musicroom.shared.dto.UserResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * "Continue with Google / Apple" buttons. Signs in, or creates the account after asking the user to accept
 * the terms (the server answers terms_required for a new account). Shows nothing if no provider is available.
 */
@Composable
fun SocialSignInButtons(
    api: MusicRoomApi,
    socialSignIn: SocialSignIn,
    enabled: Boolean,
    onBusyChange: (Boolean) -> Unit,
    onError: (UiText?) -> Unit,
    onAuthenticated: (UserResponse) -> Unit,
    onTwoFactorRequired: (String, TwoFactorMethod) -> Unit,
) {
    if (socialSignIn.providers.isEmpty()) return
    // Sign-in that is waiting for the user to accept the terms.
    var pendingTerms by remember { mutableStateOf<Pair<OAuthProvider, SocialCredential>?>(null) }
    val scope = rememberCoroutineScope()

    fun run(action: suspend () -> Unit) {
        onError(null)
        onBusyChange(true)
        scope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: TwoFactorRequiredException) {
                onTwoFactorRequired(e.challengeToken, e.method)
            } catch (e: Exception) {
                onError(UiText.of(e))
            } finally {
                onBusyChange(false)
            }
        }
    }

    suspend fun login(provider: OAuthProvider, credential: SocialCredential, acceptedTerms: Boolean) {
        try {
            onAuthenticated(
                api.oauthLogin(OAuthLoginRequest(provider, credential.idToken, acceptedTerms, credential.displayName)),
            )
        } catch (e: ApiException) {
            if (e.error == ApiErrors.TERMS_REQUIRED) pendingTerms = provider to credential else throw e
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        Text(
            stringResource(Res.string.common_or),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.textSecondary,
        )
        socialSignIn.providers.forEach { provider ->
            AppButton(
                text = stringResource(Res.string.social_continue, provider.displayName),
                onClick = {
                    run {
                        val credential = socialSignIn.signIn(provider) ?: return@run
                        login(provider, credential, acceptedTerms = false)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                variant = AppButtonVariant.Secondary,
            )
        }
    }

    pendingTerms?.let { (provider, credential) ->
        TermsDialog(
            onDismiss = { pendingTerms = null },
            onAccept = {
                pendingTerms = null
                run { login(provider, credential, acceptedTerms = true) }
            },
        )
    }
}
