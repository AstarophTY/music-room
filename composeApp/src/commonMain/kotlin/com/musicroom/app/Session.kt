package com.musicroom.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.musicroom.app.auth.AuthScreen
import com.musicroom.app.auth.SocialSignIn
import com.musicroom.app.auth.VerifyEmailScreen
import com.musicroom.app.main.MainScreen
import com.musicroom.app.settings.AppSettings
import com.musicroom.app.theme.components.AppBottomSheet
import com.musicroom.app.theme.ThemeMode
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi

/** Picks the screen for the session: loading, logged out, email to verify, or the app itself. */
@Composable
fun Session(
    api: MusicRoomApi,
    socialSignIn: SocialSignIn,
    session: SessionState,
    onSessionChange: (SessionState) -> Unit,
    themeMode: ThemeMode,
    settings: AppSettings,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    var providerChoiceSeen by remember(settings) { mutableStateOf(settings.providerChoiceSeen) }
    when (session) {
        SessionState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AppTheme.colors.accentText)
        }
        is SessionState.LoggedOut -> AuthScreen(
            api = api,
            socialSignIn = socialSignIn,
            initialMessage = session.message,
            onAuthenticated = { onSessionChange(SessionState.LoggedIn(it)) },
        )
        is SessionState.LoggedIn -> if (!session.user.emailVerified) {
            VerifyEmailScreen(
                api = api,
                user = session.user,
                onVerified = { onSessionChange(SessionState.LoggedIn(it)) },
                onLoggedOut = { onSessionChange(SessionState.LoggedOut()) },
            )
        } else {
            MainScreen(
                api = api,
                user = session.user,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                onUserChange = { onSessionChange(SessionState.LoggedIn(it)) },
                onLoggedOut = { onSessionChange(SessionState.LoggedOut()) },
            )
        }
    }
}
