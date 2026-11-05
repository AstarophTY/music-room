package com.musicroom.server.modules.spotify.entities

import com.musicroom.server.modules.users.entities.UsersTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object SpotifyAccountsTable : Table("spotify_accounts") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val spotifyUserId = varchar("spotify_user_id", 128)
    val displayName = varchar("display_name", 255).nullable()
    val product = varchar("product", 32).nullable()

    /** Both tokens are encrypted with SecretBox. */
    val accessToken = varchar("access_token", 1024)
    val refreshToken = varchar("refresh_token", 1024)
    val expiresAt = timestampWithTimeZone("expires_at")
    val scope = varchar("scope", 512).nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(userId)
}
