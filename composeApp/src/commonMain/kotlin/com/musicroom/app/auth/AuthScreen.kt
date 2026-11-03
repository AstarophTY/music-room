package com.musicroom.app.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.musicroom.app.common.UiText
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.UserResponse

/** Logged-out part of the app: login, sign-up, forgot password, 2FA code. The email is shared between them. */
@Composable
fun AuthScreen(
    api: MusicRoomApi,
    socialSignIn: SocialSignIn,
    initialMessage: UiText?,
    onAuthenticated: (UserResponse) -> Unit,
) {
    var route by rememberSaveable { mutableStateOf(AuthRoute.Login) }
    var email by rememberSaveable { mutableStateOf("") }
    // Set when a login needs a 2FA code; the route where it started is where "back" goes.
    var challengeToken by rememberSaveable { mutableStateOf<String?>(null) }
    var routeBeforeTwoFactor by rememberSaveable { mutableStateOf(AuthRoute.Login) }
    val onTwoFactorRequired: (String) -> Unit = { token ->
        challengeToken = token
        routeBeforeTwoFactor = route
        route = AuthRoute.TwoFactor
    }
    when (route) {
        AuthRoute.Login -> LoginScreen(
            api = api,
            socialSignIn = socialSignIn,
            email = email,
            onEmailChange = { email = it },
            initialMessage = initialMessage,
            onAuthenticated = onAuthenticated,
            onSignUp = { route = AuthRoute.SignUp },
            onForgotPassword = { route = AuthRoute.ForgotPassword },
            onTwoFactorRequired = onTwoFactorRequired,
        )
        AuthRoute.SignUp -> SignUpScreen(
            api = api,
            socialSignIn = socialSignIn,
            email = email,
            onEmailChange = { email = it },
            onAuthenticated = onAuthenticated,
            onLogIn = { route = AuthRoute.Login },
            onTwoFactorRequired = onTwoFactorRequired,
        )
        AuthRoute.ForgotPassword -> ForgotPasswordScreen(
            api = api,
            email = email,
            onEmailChange = { email = it },
            onBack = { route = AuthRoute.Login },
            onAuthenticated = onAuthenticated,
            onTwoFactorRequired = onTwoFactorRequired,
        )
        AuthRoute.TwoFactor -> {
            val token = challengeToken
            if (token == null) {
                LaunchedEffect(Unit) { route = AuthRoute.Login }
            } else {
                TwoFactorLoginScreen(
                    api = api,
                    challengeToken = token,
                    // The password was right, but going back means logging in again from the start.
                    onBack = { route = if (routeBeforeTwoFactor == AuthRoute.ForgotPassword) AuthRoute.Login else routeBeforeTwoFactor },
                    onAuthenticated = onAuthenticated,
                )
            }
        }
    }
}
