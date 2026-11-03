package com.musicroom.server.modules.auth.entities

import com.musicroom.server.modules.users.entities.UsersTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object EmailVerificationCodesTable : UUIDTable("email_verification_codes") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val purpose = varchar("purpose", 32)
    val codeHash = varchar("code_hash", 64)
    val attempts = integer("attempts").default(0)
    val expiresAt = timestampWithTimeZone("expires_at")
    val usedAt = timestampWithTimeZone("used_at").nullable()
    val createdAt = timestampWithTimeZone("created_at")
}
