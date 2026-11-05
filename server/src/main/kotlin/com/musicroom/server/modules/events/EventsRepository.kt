package com.musicroom.server.modules.events

import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.events.entities.Event
import com.musicroom.server.modules.events.entities.EventInvitesTable
import com.musicroom.server.modules.events.entities.EventsTable
import com.musicroom.shared.dto.CreateEventRequest
import com.musicroom.shared.dto.EventStatus
import com.musicroom.shared.dto.EventVisibility
import com.musicroom.shared.dto.EventVoteLicense
import com.musicroom.shared.dto.GeoPoint
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

/** Events and the people invited to them. */
class EventsRepository(private val db: DatabaseModule) {

    suspend fun create(ownerId: UUID, request: CreateEventRequest): Event = db.query {
        val now = OffsetDateTime.now()
        val id = EventsTable.insertAndGetId {
            it[EventsTable.ownerId] = ownerId
            it.settings(request)
            it[status] = EventStatus.WAITING.key()
            it[createdAt] = now
            it[updatedAt] = now
        }.value
        EventsTable.selectAll().where { EventsTable.id eq id }.single().toEvent()
    }

    suspend fun update(id: UUID, request: CreateEventRequest) = db.query {
        EventsTable.update({ EventsTable.id eq id }) {
            it.settings(request)
            it[updatedAt] = OffsetDateTime.now()
        }
    }

    suspend fun find(id: UUID): Event? = db.query {
        EventsTable.selectAll().where { EventsTable.id eq id }.singleOrNull()?.toEvent()
    }

    suspend fun delete(id: UUID) = db.query { EventsTable.deleteWhere { EventsTable.id eq id } }

    suspend fun setStatus(id: UUID, status: EventStatus) = db.query {
        EventsTable.update({ EventsTable.id eq id }) {
            it[EventsTable.status] = status.key()
            it[updatedAt] = OffsetDateTime.now()
        }
    }

    /** Public events, the latest first; [located] keeps those that have an area. */
    suspend fun public(limit: Int, located: Boolean = false): List<Event> = db.query {
        EventsTable.selectAll()
            .where {
                val visible = EventsTable.visibility eq EventVisibility.PUBLIC.key()
                if (located) visible and EventsTable.latitude.isNotNull() else visible
            }
            .orderBy(EventsTable.updatedAt to SortOrder.DESC)
            .limit(limit)
            .map { it.toEvent() }
    }

    suspend fun ownedBy(userId: UUID): List<Event> = db.query {
        EventsTable.selectAll().where { EventsTable.ownerId eq userId }
            .orderBy(EventsTable.updatedAt to SortOrder.DESC)
            .map { it.toEvent() }
    }

    suspend fun invitedTo(userId: UUID): List<Event> = db.query {
        val ids = EventInvitesTable.select(EventInvitesTable.eventId).where { EventInvitesTable.userId eq userId }
            .map { it[EventInvitesTable.eventId].value }
        if (ids.isEmpty()) {
            emptyList()
        } else {
            EventsTable.selectAll().where { (EventsTable.id inList ids) and (EventsTable.ownerId neq userId) }
                .orderBy(EventsTable.updatedAt to SortOrder.DESC)
                .map { it.toEvent() }
        }
    }

    /** Events whose music plays: the player follows them. */
    suspend fun playing(): List<UUID> = db.query {
        EventsTable.select(EventsTable.id).where { EventsTable.status eq EventStatus.PLAYING.key() }.map { it[EventsTable.id].value }
    }

    /** False when [userId] was already invited. */
    suspend fun invite(eventId: UUID, userId: UUID, invitedBy: UUID): Boolean = db.query {
        EventInvitesTable.insertIgnore {
            it[EventInvitesTable.eventId] = eventId
            it[EventInvitesTable.userId] = userId
            it[EventInvitesTable.invitedBy] = invitedBy
            it[createdAt] = OffsetDateTime.now()
        }.insertedCount == 1
    }

    suspend fun uninvite(eventId: UUID, userId: UUID): Boolean = db.query {
        EventInvitesTable.deleteWhere { (EventInvitesTable.eventId eq eventId) and (EventInvitesTable.userId eq userId) } == 1
    }

    suspend fun isInvited(eventId: UUID, userId: UUID): Boolean = db.query {
        EventInvitesTable.selectAll()
            .where { (EventInvitesTable.eventId eq eventId) and (EventInvitesTable.userId eq userId) }
            .count() > 0
    }

    /** Ids of the people invited, the first invited first. */
    suspend fun invited(eventId: UUID): List<UUID> = db.query {
        EventInvitesTable.select(EventInvitesTable.userId).where { EventInvitesTable.eventId eq eventId }
            .orderBy(EventInvitesTable.createdAt to SortOrder.ASC)
            .map { it[EventInvitesTable.userId].value }
    }

    /** Which of [eventIds] the user was invited to. */
    suspend fun invitedAmong(userId: UUID, eventIds: Collection<UUID>): Set<UUID> = if (eventIds.isEmpty()) {
        emptySet()
    } else {
        db.query {
            EventInvitesTable.select(EventInvitesTable.eventId)
                .where { (EventInvitesTable.userId eq userId) and (EventInvitesTable.eventId inList eventIds) }
                .map { it[EventInvitesTable.eventId].value }
                .toSet()
        }
    }

    private fun UpdateBuilder<*>.settings(request: CreateEventRequest) {
        val located = request.voteLicense == EventVoteLicense.LOCATION
        this[EventsTable.name] = request.name.trim()
        this[EventsTable.description] = request.description.trim()
        this[EventsTable.visibility] = request.visibility.key()
        this[EventsTable.voteLicense] = request.voteLicense.key()
        // The area only matters with the location license; it also places the event for "nearby".
        this[EventsTable.latitude] = request.location?.latitude
        this[EventsTable.longitude] = request.location?.longitude
        this[EventsTable.radiusMeters] = if (located || request.location != null) request.radiusMeters else null
        this[EventsTable.startsAt] = request.startsAt?.toTime()
        this[EventsTable.endsAt] = request.endsAt?.toTime()
    }

    private fun Long.toTime() = OffsetDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneOffset.UTC)

    private fun Enum<*>.key() = name.lowercase()

    private fun ResultRow.toEvent(): Event {
        val latitude = this[EventsTable.latitude]
        val longitude = this[EventsTable.longitude]
        return Event(
            id = this[EventsTable.id].value,
            ownerId = this[EventsTable.ownerId].value,
            name = this[EventsTable.name],
            description = this[EventsTable.description],
            visibility = EventVisibility.valueOf(this[EventsTable.visibility].uppercase()),
            voteLicense = EventVoteLicense.valueOf(this[EventsTable.voteLicense].uppercase()),
            location = if (latitude != null && longitude != null) GeoPoint(latitude, longitude) else null,
            radiusMeters = this[EventsTable.radiusMeters],
            startsAt = this[EventsTable.startsAt],
            endsAt = this[EventsTable.endsAt],
            status = EventStatus.valueOf(this[EventsTable.status].uppercase()),
        )
    }
}
