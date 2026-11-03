package com.musicroom.shared

/** Login succeeded but the account has 2FA: call MusicRoomApi.verifyTwoFactor with a code and [challengeToken]. */
class TwoFactorRequiredException(val challengeToken: String) : RuntimeException("Two-factor code required")
