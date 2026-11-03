package com.musicroom.server.modules.users.entities

import com.musicroom.shared.dto.AvatarColor
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
    val twoFactorLockedUntil: OffsetDateTime? = null,
    val avatarColor: AvatarColor = AvatarColor.YELLOW,
    val avatarUpdatedAt: OffsetDateTime? = null,
) {
    val twoFactorEnabled: Boolean get() = totpSecret != null

    fun toResponse() = UserResponse(
        id = id.toString(),
        email = email,
        displayName = displayName,
        emailVerified = emailVerified,
        twoFactorEnabled = twoFactorEnabled,
        avatarColor = avatarColor,
        avatarVersion = avatarUpdatedAt?.toInstant()?.toEpochMilli(),
    )
}
