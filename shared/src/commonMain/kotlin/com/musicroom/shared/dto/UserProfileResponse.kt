package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Another user's profile page. */
@Serializable
data class UserProfileResponse(
    val user: PublicUserResponse,
    val socialLinks: List<SocialLink> = emptyList(),
    val friendCount: Int = 0,
    val publicInfo: String = "",
    /** Only sent to the user's friends. */
    val friendsInfo: String? = null,
    val musicPreferences: List<String> = emptyList(),
)
