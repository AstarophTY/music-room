package com.musicroom.app.profile

/** Pages opened from the avatar. [parent] is where the back button goes (null: back to the tabs). */
enum class ProfileRoute(val parent: ProfileRoute?) {
    Profile(null),
    EditProfile(Profile),
    ProfileDetails(Profile),
    SocialLinks(Profile),
    ChangePassword(Profile),
    TwoFactor(Profile),
    Appearance(Profile),
    AudioProviders(Profile),
    MySongs(Profile),
    Premium(Profile),
}
