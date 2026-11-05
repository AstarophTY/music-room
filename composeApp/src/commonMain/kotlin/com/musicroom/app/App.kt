package com.musicroom.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import com.musicroom.app.auth.SocialSignIn
import com.musicroom.app.common.UiText
import com.musicroom.app.common.WindowSize
import com.musicroom.app.settings.AppSettings
import com.musicroom.app.theme.AppTheme
import com.musicroom.app.theme.ThemeMode
import com.musicroom.app.theme.tokens.AppTheme as Tokens
import com.musicroom.shared.MusicRoomApi
import kotlinx.coroutines.CancellationException

/**
 * Root of the app. [onDarkThemeChange] lets the platform match its status bar icons to the theme.
 */
@Composable
fun App(
    api: MusicRoomApi,
    socialSignIn: SocialSignIn = SocialSignIn.Unavailable,
    settings: AppSettings = remember { AppSettings.InMemory() },
    onDarkThemeChange: (Boolean) -> Unit = {},
) {
    var session by remember { mutableStateOf<SessionState>(SessionState.Loading) }
    var themeMode by remember { mutableStateOf(settings.themeMode) }
    val dark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val language = Locale.current.language
    SideEffect { api.language = language }
    LaunchedEffect(dark) { onDarkThemeChange(dark) }

    LaunchedEffect(api) {
        session = try {
            api.restoreSession()?.let { SessionState.LoggedIn(it) } ?: SessionState.LoggedOut()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SessionState.LoggedOut(message = UiText.of(e))
        }
    }

    AppTheme(dark = dark) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            CompositionLocalProvider(WindowSize.Local provides WindowSize.fromWidth(maxWidth)) {
                Surface(modifier = Modifier.fillMaxSize(), color = Tokens.colors.background) {
                    Session(api, socialSignIn, session, { session = it }, themeMode, settings) {
                        themeMode = it
                        settings.themeMode = it
                    }
                }
            }
        }
    }
}
