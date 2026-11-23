package com.musicroom.server.modules.users.entities

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object UserDetailsTable : Table("user_details") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val publicInfo = text("public_info")
    val friendsInfo = text("friends_info")
    val privateInfo = text("private_info")

    /** A JSON array of strings. */
    val musicPreferences = text("music_preferences")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(userId)
}
