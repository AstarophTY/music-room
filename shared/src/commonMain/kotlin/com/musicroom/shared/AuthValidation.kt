package com.musicroom.shared

/** Input rules shared by the app (instant feedback) and the server (source of truth). */
object AuthValidation {
    const val PASSWORD_MIN_LENGTH = 8

    /** bcrypt only uses the first 72 bytes of a password. */
    const val PASSWORD_MAX_BYTES = 72
    const val DISPLAY_NAME_MAX_LENGTH = 64
    const val EMAIL_MAX_LENGTH = 320
    const val VERIFICATION_CODE_LENGTH = 6

    private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun normalizeEmail(email: String): String = email.trim().lowercase()

    fun emailError(email: String): ValidationError? = when {
        email.isBlank() -> ValidationError.EMAIL_REQUIRED
        email.length > EMAIL_MAX_LENGTH -> ValidationError.EMAIL_TOO_LONG
        !emailRegex.matches(email) -> ValidationError.EMAIL_INVALID
        else -> null
    }

    fun passwordError(password: String): ValidationError? = when {
        password.length < PASSWORD_MIN_LENGTH -> ValidationError.PASSWORD_TOO_SHORT
        password.encodeToByteArray().size > PASSWORD_MAX_BYTES -> ValidationError.PASSWORD_TOO_LONG
        password.none { it.isLetter() } || password.none { it.isDigit() } -> ValidationError.PASSWORD_TOO_WEAK
        else -> null
    }

    fun displayNameError(displayName: String): ValidationError? = when {
        displayName.isBlank() -> ValidationError.DISPLAY_NAME_REQUIRED
        displayName.trim().length > DISPLAY_NAME_MAX_LENGTH -> ValidationError.DISPLAY_NAME_TOO_LONG
        else -> null
    }

    /** Client-side only: the server receives the password once. */
    fun passwordConfirmationError(password: String, confirmation: String): ValidationError? = when {
        confirmation.isEmpty() -> ValidationError.PASSWORD_CONFIRMATION_REQUIRED
        confirmation != password -> ValidationError.PASSWORD_MISMATCH
        else -> null
    }

    fun termsError(accepted: Boolean): ValidationError? = if (accepted) null else ValidationError.TERMS_NOT_ACCEPTED

    fun normalizeVerificationCode(code: String): String = code.filter { !it.isWhitespace() }

    fun verificationCodeError(code: String): ValidationError? =
        if (code.length == VERIFICATION_CODE_LENGTH && code.all { it in '0'..'9' }) null else ValidationError.CODE_FORMAT
}
