package com.musicroom.shared.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** How the second login step works. An account uses at most one method, and none until the user turns one on. */
@Serializable
enum class TwoFactorMethod {
    /** Code from an authenticator app (TOTP), or a recovery code. */
    @SerialName("app") APP,

    /** Code emailed at each login. */
    @SerialName("email") EMAIL,
}
