package com.musicroom.server.modules.auth.entities

import com.musicroom.server.modules.users.entities.UsersTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object OAuthAccountsTable : UUIDTable("oauth_accounts") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val provider = varchar("provider", 16)
    val providerUserId = varchar("provider_user_id", 255)
    val email = varchar("email", 320).nullable()
    val createdAt = timestampWithTimeZone("created_at")
}
