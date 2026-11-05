package com.musicroom.app.auth

import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.signup_email_subtitle
import com.musicroom.app.generated.resources.signup_email_title
import com.musicroom.app.generated.resources.signup_password_subtitle
import com.musicroom.app.generated.resources.signup_password_title
import com.musicroom.app.generated.resources.signup_profile_subtitle
import com.musicroom.app.generated.resources.signup_profile_title
import org.jetbrains.compose.resources.StringResource

/** Pages of the sign-up flow, in order. */
enum class SignUpStep(val title: StringResource, val subtitle: StringResource) {
    Email(Res.string.signup_email_title, Res.string.signup_email_subtitle),
    Password(Res.string.signup_password_title, Res.string.signup_password_subtitle),
    Profile(Res.string.signup_profile_title, Res.string.signup_profile_subtitle),
}
