package com.musicroom.server.modules.users.entities

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object UserAvatarsTable : Table("user_avatars") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val image = binary("image")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(userId)
}
