package com.musicroom.app.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.musicroom.app.common.UiText
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.UserResponse

/** Logged-out part of the app: login, sign-up and forgot password. The email is shared between them. */
@Composable
fun AuthScreen(
    api: MusicRoomApi,
    socialSignIn: SocialSignIn,
    initialMessage: UiText?,
    onAuthenticated: (UserResponse) -> Unit,
) {
    var route by rememberSaveable { mutableStateOf(AuthRoute.Login) }
    var email by rememberSaveable { mutableStateOf("") }
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
        )
        AuthRoute.SignUp -> SignUpScreen(
            api = api,
            socialSignIn = socialSignIn,
            email = email,
            onEmailChange = { email = it },
            onAuthenticated = onAuthenticated,
            onLogIn = { route = AuthRoute.Login },
        )
        AuthRoute.ForgotPassword -> ForgotPasswordScreen(
            api = api,
            email = email,
            onEmailChange = { email = it },
            onBack = { route = AuthRoute.Login },
            onAuthenticated = onAuthenticated,
        )
    }
}
