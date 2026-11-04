package com.musicroom.shared

import com.musicroom.shared.dto.TwoFactorMethod

/** Login succeeded but the account has 2FA: call MusicRoomApi.verifyTwoFactor with a code and [challengeToken]. */
class TwoFactorRequiredException(
    val challengeToken: String,
    val method: TwoFactorMethod = TwoFactorMethod.APP,
) : RuntimeException("Two-factor code required")
