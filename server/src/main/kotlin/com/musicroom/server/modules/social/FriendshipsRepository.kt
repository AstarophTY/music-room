package com.musicroom.server.modules.social

import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.social.entities.FriendshipsTable
import java.time.OffsetDateTime
import java.util.UUID
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

/** Friend requests and friendships: one row per pair of users. */
class FriendshipsRepository(private val db: DatabaseModule) {

    /** The row of a pair: who asked, who was asked, and whether it was accepted. */
    data class Link(val requesterId: UUID, val addresseeId: UUID, val accepted: Boolean)

    /** The link between [a] and [b], whoever asked; null when there is none. */
    suspend fun find(a: UUID, b: UUID): Link? = db.query {
        FriendshipsTable.selectAll().where { pair(a, b) }.singleOrNull()?.toLink()
    }

    /** Every link of [userId]. */
    suspend fun links(userId: UUID): List<Link> = db.query {
        FriendshipsTable.selectAll()
            .where { (FriendshipsTable.requesterId eq userId) or (FriendshipsTable.addresseeId eq userId) }
            .orderBy(FriendshipsTable.createdAt to SortOrder.DESC)
            .map { it.toLink() }
    }

    /** False when the pair already has a link (in either direction). */
    suspend fun request(from: UUID, to: UUID): Boolean = db.query {
        FriendshipsTable.insertIgnore {
            it[requesterId] = from
            it[addresseeId] = to
            it[status] = FriendshipsTable.PENDING
            it[createdAt] = OffsetDateTime.now()
        }.insertedCount == 1
    }

    /** The addressee accepts. False when there was no such pending request. */
    suspend fun accept(requester: UUID, addressee: UUID): Boolean = db.query {
        FriendshipsTable.update({
            (FriendshipsTable.requesterId eq requester) and (FriendshipsTable.addresseeId eq addressee) and
                (FriendshipsTable.status eq FriendshipsTable.PENDING)
        }) {
            it[status] = FriendshipsTable.ACCEPTED
            it[acceptedAt] = OffsetDateTime.now()
        } == 1
    }

    /** Removes the friendship or the request between [a] and [b]. False when there was none. */
    suspend fun delete(a: UUID, b: UUID): Boolean = db.query {
        FriendshipsTable.deleteWhere { pair(a, b) } >= 1
    }

    private fun pair(a: UUID, b: UUID): Op<Boolean> =
        ((FriendshipsTable.requesterId eq a) and (FriendshipsTable.addresseeId eq b)) or
            ((FriendshipsTable.requesterId eq b) and (FriendshipsTable.addresseeId eq a))

    private fun ResultRow.toLink() = Link(
        requesterId = this[FriendshipsTable.requesterId].value,
        addresseeId = this[FriendshipsTable.addresseeId].value,
        accepted = this[FriendshipsTable.status] == FriendshipsTable.ACCEPTED,
    )
}
