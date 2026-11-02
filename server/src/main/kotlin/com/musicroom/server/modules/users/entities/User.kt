package com.musicroom.server.modules.users.entities

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
) {
    fun toResponse() = UserResponse(
        id = id.toString(),
        email = email,
        displayName = displayName,
        emailVerified = emailVerified,
    )
}
