package com.musicroom.server.modules.premium.entities

import com.musicroom.server.modules.users.entities.UsersTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object SubscriptionsTable : Table("subscriptions") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val stripeCustomerId = varchar("stripe_customer_id", 64)
    val stripeSubscriptionId = varchar("stripe_subscription_id", 64).nullable()
    val status = varchar("status", 32)
    val currentPeriodEnd = timestampWithTimeZone("current_period_end").nullable()
    val cancelAtPeriodEnd = bool("cancel_at_period_end")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(userId)
}
