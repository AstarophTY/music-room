package com.musicroom.server.modules.users.entities

import com.musicroom.shared.dto.AvatarColor
import com.musicroom.shared.dto.SocialLink
import com.musicroom.shared.dto.TwoFactorMethod
import com.musicroom.shared.dto.UserResponse
import java.time.OffsetDateTime
import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val passwordHash: String?,
    val displayName: String,
    val emailVerified: Boolean,
    val createdAt: OffsetDateTime,
    val totpSecret: String? = null,
    val totpPendingSecret: String? = null,
    val totpLastStep: Long? = null,
    val emailTwoFactorEnabled: Boolean = false,
    val twoFactorLockedUntil: OffsetDateTime? = null,
    val avatarColor: AvatarColor = AvatarColor.YELLOW,
    val avatarUpdatedAt: OffsetDateTime? = null,
    val socialLinks: List<SocialLink> = emptyList(),
    val spotifyConnected: Boolean = false,
) {
    /** Null when 2FA is off. */
    val twoFactorMethod: TwoFactorMethod?
        get() = when {
            totpSecret != null -> TwoFactorMethod.APP
            emailTwoFactorEnabled -> TwoFactorMethod.EMAIL
            else -> null
        }

    val twoFactorEnabled: Boolean get() = twoFactorMethod != null

    fun toResponse() = UserResponse(
        id = id.toString(),
        email = email,
        displayName = displayName,
        emailVerified = emailVerified,
        twoFactorEnabled = twoFactorEnabled,
        twoFactorMethod = twoFactorMethod,
        avatarColor = avatarColor,
        avatarVersion = avatarUpdatedAt?.toInstant()?.toEpochMilli(),
        socialLinks = socialLinks,
        spotifyConnected = spotifyConnected,
    )
}
