package com.musicroom.app.auth

/** The provider's sign-in failed on the device. [notConfigured]: the app has no client ID for it. */
class SocialSignInException(message: String, val notConfigured: Boolean = false) : RuntimeException(message)
