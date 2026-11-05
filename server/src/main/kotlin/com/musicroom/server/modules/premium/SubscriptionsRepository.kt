package com.musicroom.server.modules.premium

import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.premium.entities.Subscription
import com.musicroom.server.modules.premium.entities.SubscriptionsTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.OffsetDateTime
import java.util.UUID

/** Users' Stripe customers and the state of their Premium subscription. */
class SubscriptionsRepository(private val db: DatabaseModule) {

    suspend fun find(userId: UUID): Subscription? = db.query {
        SubscriptionsTable.selectAll().where { SubscriptionsTable.userId eq userId }.singleOrNull()?.toSubscription()
    }

    suspend fun findByCustomer(customerId: String): Subscription? = db.query {
        SubscriptionsTable.selectAll().where { SubscriptionsTable.stripeCustomerId eq customerId }.singleOrNull()?.toSubscription()
    }

    /** Remembers the Stripe customer of [userId]; the first one stays if two requests race. */
    suspend fun createCustomer(userId: UUID, customerId: String): Subscription = db.query {
        SubscriptionsTable.insertIgnore {
            it[SubscriptionsTable.userId] = userId
            it[stripeCustomerId] = customerId
            it[status] = NO_SUBSCRIPTION
            it[cancelAtPeriodEnd] = false
            it[updatedAt] = OffsetDateTime.now()
        }
        SubscriptionsTable.selectAll().where { SubscriptionsTable.userId eq userId }.single().toSubscription()
    }

    /** Saves what Stripe says of the subscription of the customer [customerId]. */
    suspend fun update(customerId: String, subscriptionId: String, status: String, periodEnd: OffsetDateTime?, cancelAtPeriodEnd: Boolean) {
        db.query {
            SubscriptionsTable.update({ SubscriptionsTable.stripeCustomerId eq customerId }) {
                it[stripeSubscriptionId] = subscriptionId
                it[SubscriptionsTable.status] = status
                it[currentPeriodEnd] = periodEnd
                it[SubscriptionsTable.cancelAtPeriodEnd] = cancelAtPeriodEnd
                it[updatedAt] = OffsetDateTime.now()
            }
        }
    }

    private fun ResultRow.toSubscription() = Subscription(
        userId = this[SubscriptionsTable.userId].value,
        customerId = this[SubscriptionsTable.stripeCustomerId],
        subscriptionId = this[SubscriptionsTable.stripeSubscriptionId],
        status = this[SubscriptionsTable.status],
        currentPeriodEnd = this[SubscriptionsTable.currentPeriodEnd],
        cancelAtPeriodEnd = this[SubscriptionsTable.cancelAtPeriodEnd],
        updatedAt = this[SubscriptionsTable.updatedAt],
    )

    companion object {
        const val NO_SUBSCRIPTION = "none"
    }
}
