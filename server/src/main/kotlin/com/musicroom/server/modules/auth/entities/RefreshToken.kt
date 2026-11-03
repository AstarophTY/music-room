package com.musicroom.server.modules.auth.entities

import java.time.OffsetDateTime
import java.util.UUID

data class RefreshToken(
    val id: UUID,
    val userId: UUID,
    val expiresAt: OffsetDateTime,
    val revokedAt: OffsetDateTime?,
)
