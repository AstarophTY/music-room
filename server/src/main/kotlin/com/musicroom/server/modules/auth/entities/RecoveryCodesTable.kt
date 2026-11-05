package com.musicroom.server.modules.auth.entities

import com.musicroom.server.modules.users.entities.UsersTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object RecoveryCodesTable : UUIDTable("recovery_codes") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val codeHash = varchar("code_hash", 64)
    val usedAt = timestampWithTimeZone("used_at").nullable()
    val createdAt = timestampWithTimeZone("created_at")
}
