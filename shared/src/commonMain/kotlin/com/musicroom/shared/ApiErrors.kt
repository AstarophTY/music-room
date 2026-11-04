package com.musicroom.shared

/**
 * Machine-readable `error` values of ErrorResponse. The app translates them; unknown ones show the server's
 * English message. Input errors use ValidationError codes.
 */
object ApiErrors {
    const val INVALID_CREDENTIALS = "invalid_credentials"
    const val EMAIL_TAKEN = "email_taken"

    /** OAuth sign-in for a new account: ask the user to accept the terms, then retry with acceptedTerms = true. */
    const val TERMS_REQUIRED = "terms_required"
    const val WRONG_CURRENT_PASSWORD = "wrong_current_password"
    const val INVALID_CODE = "invalid_code"
    const val CODE_EXPIRED = "code_expired"
    const val TOO_MANY_ATTEMPTS = "too_many_attempts"
    const val EMAIL_ALREADY_VERIFIED = "email_already_verified"
    const val RESEND_TOO_SOON = "resend_too_soon"
    const val EMAIL_UNAVAILABLE = "email_unavailable"
    const val OAUTH_NOT_CONFIGURED = "oauth_not_configured"
    const val INVALID_OAUTH_TOKEN = "invalid_oauth_token"
    const val OAUTH_EMAIL_UNVERIFIED = "oauth_email_unverified"
    const val OAUTH_ALREADY_LINKED = "oauth_already_linked"
    const val TOO_MANY_REQUESTS = "too_many_requests"
    const val INVALID_TWO_FACTOR_CODE = "invalid_two_factor_code"
    const val TWO_FACTOR_LOCKED = "two_factor_locked"
    const val TWO_FACTOR_CHALLENGE_EXPIRED = "two_factor_challenge_expired"
    const val TWO_FACTOR_ALREADY_ENABLED = "two_factor_already_enabled"
    const val TWO_FACTOR_NOT_ENABLED = "two_factor_not_enabled"
    const val TWO_FACTOR_SETUP_REQUIRED = "two_factor_setup_required"
    const val SPOTIFY_NOT_CONFIGURED = "spotify_not_configured"
    const val SPOTIFY_NOT_CONNECTED = "spotify_not_connected"
    const val SPOTIFY_UNAVAILABLE = "spotify_unavailable"
    const val SPOTIFY_RATE_LIMITED = "spotify_rate_limited"
    const val INVALID_IMAGE = "invalid_image"
    const val IMAGE_TOO_LARGE = "image_too_large"

    /** Set by the client when the refresh token is no longer valid. */
    const val SESSION_EXPIRED = "session_expired"
}
