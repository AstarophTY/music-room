package com.musicroom.app.profile

/** Pages opened from the avatar. [parent] is where the back button goes (null: back to the tabs). */
enum class ProfileRoute(val parent: ProfileRoute?) {
    Profile(null),
    EditProfile(Profile),
    ChangePassword(Profile),
    Appearance(Profile),
}
