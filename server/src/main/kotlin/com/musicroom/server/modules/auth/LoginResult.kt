package com.musicroom.server.modules.auth

import com.musicroom.shared.dto.AuthResponse
import com.musicroom.shared.dto.TwoFactorChallengeResponse

/** Outcome of a login: tokens, or a 2FA challenge when the account has two-factor authentication. */
sealed interface LoginResult {
    data class Success(val auth: AuthResponse) : LoginResult

    data class TwoFactorRequired(val challenge: TwoFactorChallengeResponse) : LoginResult
}
