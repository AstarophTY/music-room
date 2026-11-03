package com.musicroom.server.modules.users.entities

import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object UsersTable : UUIDTable("users") {
    val email = varchar("email", 320).uniqueIndex()
    val passwordHash = varchar("password_hash", 255).nullable()
    val displayName = varchar("display_name", 64)
    val emailVerified = bool("email_verified").default(false)
    val termsAcceptedAt = timestampWithTimeZone("terms_accepted_at").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}
