package com.musicroom.app.common

import androidx.compose.runtime.Composable
import com.musicroom.app.auth.SocialSignInException
import com.musicroom.app.generated.resources.*
import com.musicroom.app.generated.resources.Res
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.ApiException
import com.musicroom.shared.AuthValidation
import com.musicroom.shared.ValidationError
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Text to show to the user, translated when it is displayed (state can hold it outside composition). */
sealed interface UiText {
    @Composable
    fun resolve(): String

    data class Resource(val resource: StringResource, val args: List<Any> = emptyList()) : UiText {
        @Composable
        override fun resolve(): String = stringResource(resource, *args.toTypedArray())
    }

    /** Text that has no translation, e.g. an unknown server message. */
    data class Raw(val text: String) : UiText {
        @Composable
        override fun resolve(): String = text
    }

    companion object {
        fun of(resource: StringResource, vararg args: Any): UiText = Resource(resource, args.toList())

        fun of(error: ValidationError): UiText = when (error) {
            ValidationError.EMAIL_REQUIRED -> of(Res.string.validation_email_required)
            ValidationError.EMAIL_TOO_LONG -> of(Res.string.validation_email_too_long)
            ValidationError.EMAIL_INVALID -> of(Res.string.validation_email_invalid)
            ValidationError.PASSWORD_TOO_SHORT ->
                of(Res.string.validation_password_too_short, AuthValidation.PASSWORD_MIN_LENGTH)
            ValidationError.PASSWORD_TOO_LONG -> of(Res.string.validation_password_too_long)
            ValidationError.PASSWORD_TOO_WEAK -> of(Res.string.validation_password_too_weak)
            ValidationError.PASSWORD_CONFIRMATION_REQUIRED -> of(Res.string.validation_password_confirmation_required)
            ValidationError.PASSWORD_MISMATCH -> of(Res.string.validation_password_mismatch)
            ValidationError.DISPLAY_NAME_REQUIRED -> of(Res.string.validation_display_name_required)
            ValidationError.DISPLAY_NAME_TOO_LONG -> of(Res.string.validation_display_name_too_long)
            ValidationError.TERMS_NOT_ACCEPTED -> of(Res.string.validation_terms_not_accepted)
            ValidationError.SOCIAL_HANDLE_INVALID -> of(Res.string.validation_social_handle_invalid)
            ValidationError.PLAYLIST_NAME_REQUIRED -> of(Res.string.validation_playlist_name_required)
            ValidationError.PLAYLIST_NAME_TOO_LONG -> of(Res.string.validation_playlist_name_too_long)
            ValidationError.EVENT_NAME_REQUIRED -> of(Res.string.validation_event_name_required)
            ValidationError.EVENT_NAME_TOO_LONG -> of(Res.string.validation_event_name_too_long)
            ValidationError.EVENT_DESCRIPTION_TOO_LONG -> of(Res.string.validation_event_description_too_long)
            ValidationError.EVENT_AREA_REQUIRED -> of(Res.string.validation_event_area_required)
            ValidationError.EVENT_HOURS_INVALID -> of(Res.string.validation_event_hours_invalid)
            ValidationError.CODE_FORMAT -> of(Res.string.validation_code_format, AuthValidation.VERIFICATION_CODE_LENGTH)
        }

        /** Translates a server error code, or falls back to the server's message. */
        fun of(error: Throwable): UiText = when (error) {
            is ApiException -> apiErrors[error.error]?.let { of(it) }
                ?: ValidationError.entries.firstOrNull { it.code == error.error }?.let { of(it) }
                ?: Raw(error.message ?: error.error)
            is SocialSignInException ->
                of(if (error.notConfigured) Res.string.error_social_not_configured else Res.string.error_social_failed)
            else -> of(Res.string.error_network)
        }

        private val apiErrors = mapOf(
            ApiErrors.INVALID_CREDENTIALS to Res.string.error_invalid_credentials,
            ApiErrors.EMAIL_TAKEN to Res.string.error_email_taken,
            ApiErrors.DISPLAY_NAME_TAKEN to Res.string.error_display_name_taken,
            ApiErrors.NOT_FRIENDS to Res.string.error_not_friends,
            ApiErrors.EVENT_VOTE_NOT_ALLOWED to Res.string.error_event_vote_not_allowed,
            ApiErrors.EVENT_LOCATION_REQUIRED to Res.string.error_event_location_required,
            ApiErrors.EVENT_OUT_OF_AREA to Res.string.error_event_out_of_area,
            ApiErrors.EVENT_CLOSED to Res.string.error_event_closed,
            ApiErrors.EVENT_PROPOSAL_LIMIT to Res.string.error_event_proposal_limit,
            ApiErrors.TERMS_REQUIRED to Res.string.error_terms_required,
            ApiErrors.WRONG_CURRENT_PASSWORD to Res.string.error_wrong_current_password,
            ApiErrors.INVALID_CODE to Res.string.error_invalid_code,
            ApiErrors.CODE_EXPIRED to Res.string.error_code_expired,
            ApiErrors.TOO_MANY_ATTEMPTS to Res.string.error_too_many_attempts,
            ApiErrors.EMAIL_ALREADY_VERIFIED to Res.string.error_email_already_verified,
            ApiErrors.RESEND_TOO_SOON to Res.string.error_resend_too_soon,
            ApiErrors.EMAIL_UNAVAILABLE to Res.string.error_email_unavailable,
            ApiErrors.OAUTH_NOT_CONFIGURED to Res.string.error_oauth_not_configured,
            ApiErrors.INVALID_OAUTH_TOKEN to Res.string.error_invalid_oauth_token,
            ApiErrors.OAUTH_EMAIL_UNVERIFIED to Res.string.error_oauth_email_unverified,
            ApiErrors.OAUTH_ALREADY_LINKED to Res.string.error_oauth_already_linked,
            ApiErrors.TOO_MANY_REQUESTS to Res.string.error_too_many_requests,
            ApiErrors.SESSION_EXPIRED to Res.string.error_session_expired,
            ApiErrors.INVALID_TWO_FACTOR_CODE to Res.string.error_invalid_two_factor_code,
            ApiErrors.TWO_FACTOR_LOCKED to Res.string.error_two_factor_locked,
            ApiErrors.TWO_FACTOR_CHALLENGE_EXPIRED to Res.string.error_two_factor_challenge_expired,
            ApiErrors.TWO_FACTOR_ALREADY_ENABLED to Res.string.error_two_factor_already_enabled,
            ApiErrors.TWO_FACTOR_NOT_ENABLED to Res.string.error_two_factor_not_enabled,
            ApiErrors.TWO_FACTOR_SETUP_REQUIRED to Res.string.error_two_factor_setup_required,
            ApiErrors.TRACK_ALREADY_IN_PLAYLIST to Res.string.error_track_already_in_playlist,
            ApiErrors.PLAYLIST_FULL to Res.string.error_playlist_full,
            ApiErrors.USER_NOT_FOUND to Res.string.error_user_not_found,
            ApiErrors.ALREADY_MEMBER to Res.string.error_already_member,
            ApiErrors.PERSONAL_PLAYLIST_READ_ONLY to Res.string.error_personal_playlist_read_only,
            ApiErrors.INVALID_IMAGE to Res.string.error_invalid_image,
            ApiErrors.IMAGE_TOO_LARGE to Res.string.error_image_too_large,
            ApiErrors.SPOTIFY_NOT_CONFIGURED to Res.string.error_spotify_not_configured,
            ApiErrors.SPOTIFY_NOT_CONNECTED to Res.string.error_spotify_not_connected,
            ApiErrors.SPOTIFY_UNAVAILABLE to Res.string.error_spotify_unavailable,
            ApiErrors.SPOTIFY_RATE_LIMITED to Res.string.error_spotify_rate_limited,
            ApiErrors.SPOTIFY_NO_DEVICE to Res.string.error_spotify_no_device,
            ApiErrors.SPOTIFY_PREMIUM_REQUIRED to Res.string.error_spotify_premium_required,
            ApiErrors.SPOTIFY_PLAYLIST_RESTRICTED to Res.string.error_spotify_playlist_restricted,
            ApiErrors.PREMIUM_REQUIRED to Res.string.error_premium_required,
            ApiErrors.ALREADY_PREMIUM to Res.string.error_already_premium,
            ApiErrors.BILLING_NOT_CONFIGURED to Res.string.error_billing_not_configured,
            ApiErrors.BILLING_UNAVAILABLE to Res.string.error_billing_unavailable,
            ApiErrors.INVALID_AUDIO to Res.string.error_invalid_audio,
            ApiErrors.AUDIO_TOO_LARGE to Res.string.error_audio_too_large,
            ApiErrors.HOSTED_TRACK_LIMIT to Res.string.error_hosted_track_limit,
            ApiErrors.HOSTED_TRACK_NOT_ALLOWED to Res.string.error_hosted_track_not_allowed,
        )
    }
}
