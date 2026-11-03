package com.musicroom.shared

/**
 * Why an input was refused. [code] is sent as the `error` field of the API response,
 * so the app can show a translated message; [message] is the English fallback.
 */
enum class ValidationError(val code: String, val message: String) {
    EMAIL_REQUIRED("email_required", "Email is required"),
    EMAIL_TOO_LONG("email_too_long", "Email is too long"),
    EMAIL_INVALID("email_invalid", "Email is invalid"),
    PASSWORD_TOO_SHORT("password_too_short", "Password must be at least ${AuthValidation.PASSWORD_MIN_LENGTH} characters"),
    PASSWORD_TOO_LONG("password_too_long", "Password is too long"),
    PASSWORD_TOO_WEAK("password_too_weak", "Password must contain at least one letter and one digit"),
    PASSWORD_CONFIRMATION_REQUIRED("password_confirmation_required", "Confirm your password"),
    PASSWORD_MISMATCH("password_mismatch", "Passwords do not match"),
    DISPLAY_NAME_REQUIRED("display_name_required", "Display name is required"),
    DISPLAY_NAME_TOO_LONG("display_name_too_long", "Display name is too long"),
    TERMS_NOT_ACCEPTED("terms_not_accepted", "You must accept the terms of use to create an account"),
    CODE_FORMAT("code_format", "Enter the ${AuthValidation.VERIFICATION_CODE_LENGTH}-digit code from the email"),
}
