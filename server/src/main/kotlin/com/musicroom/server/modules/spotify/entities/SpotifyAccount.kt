package com.musicroom.server.modules.spotify.entities

import java.time.OffsetDateTime
import java.util.UUID

/** Tokens here are decrypted. */
data class SpotifyAccount(
    val userId: UUID,
    val spotifyUserId: String,
    val displayName: String?,
    val product: String?,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: OffsetDateTime,
)
