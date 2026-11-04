package com.musicroom.server.modules.users.entities

import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object UsersTable : UUIDTable("users") {
    val email = varchar("email", 320).uniqueIndex()
    val passwordHash = varchar("password_hash", 255).nullable()
    val displayName = varchar("display_name", 64)
    val emailVerified = bool("email_verified").default(false)
    val termsAcceptedAt = timestampWithTimeZone("terms_accepted_at").nullable()

    /** Encrypted with SecretBox. Null when 2FA is off. */
    val totpSecret = varchar("totp_secret", 255).nullable()

    /** Secret being set up, until the user confirms it with a first code. */
    val totpPendingSecret = varchar("totp_pending_secret", 255).nullable()
    val totpLastStep = long("totp_last_step").nullable()
    /** Second step by emailed code. Never on together with the TOTP secret. */
    val emailTwoFactorEnabled = bool("email_two_factor_enabled").default(false)
    val twoFactorFailures = integer("two_factor_failures").default(0)
    val twoFactorLockedUntil = timestampWithTimeZone("two_factor_locked_until").nullable()

    val avatarColor = varchar("avatar_color", 16).default("yellow")
    val avatarUpdatedAt = timestampWithTimeZone("avatar_updated_at").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}
