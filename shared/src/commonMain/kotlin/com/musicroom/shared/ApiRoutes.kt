package com.musicroom.shared

object ApiRoutes {
    const val HEALTH = "/health"
    const val API_V1 = "/api/v1"
    const val INFO = "$API_V1/info"

    const val AUTH = "$API_V1/auth"
    const val AUTH_REGISTER = "$AUTH/register"
    const val AUTH_LOGIN = "$AUTH/login"
    const val AUTH_REFRESH = "$AUTH/refresh"
    const val AUTH_LOGOUT = "$AUTH/logout"
    const val AUTH_PASSWORD = "$AUTH/password"
    const val AUTH_PASSWORD_FORGOT = "$AUTH/password/forgot"
    const val AUTH_PASSWORD_RESET = "$AUTH/password/reset"
    const val AUTH_OAUTH = "$AUTH/oauth"
    const val AUTH_EMAIL_VERIFY = "$AUTH/email/verify"
    const val AUTH_EMAIL_RESEND = "$AUTH/email/resend"
    const val AUTH_2FA_VERIFY = "$AUTH/2fa/verify"
    const val AUTH_2FA_SETUP = "$AUTH/2fa/setup"
    const val AUTH_2FA_ENABLE = "$AUTH/2fa/enable"
    const val AUTH_2FA_DISABLE = "$AUTH/2fa/disable"

    const val USERS = "$API_V1/users"
    const val USERS_ME = "$USERS/me"
    const val USERS_ME_AVATAR = "$USERS_ME/avatar"
    const val USER_AVATAR = "$USERS/{id}/avatar"

    fun userAvatar(userId: String) = "$USERS/$userId/avatar"
}
