package com.musicroom.server.modules.users.entities

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object UserSocialLinksTable : Table("user_social_links") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val network = varchar("network", 16)
    val handle = varchar("handle", 64)
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(userId, network)
}
