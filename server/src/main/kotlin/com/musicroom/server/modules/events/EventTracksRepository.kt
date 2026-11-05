package com.musicroom.server.modules.events

import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.events.entities.EventTrack
import com.musicroom.server.modules.events.entities.EventTracksTable
import com.musicroom.server.modules.events.entities.EventVotesTable
import com.musicroom.shared.MusicRoomJson
import com.musicroom.shared.dto.TrackResponse
import java.time.OffsetDateTime
import java.util.UUID
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.minus
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.insertIgnoreAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

/**
 * Songs proposed in events and their votes. A vote is a row of event_votes (unique per user and song) and the
 * song's vote count changes in the same transaction, by an atomic `votes = votes + 1`: two people voting at the
 * same time both count, and the same person voting twice counts once.
 */
class EventTracksRepository(private val db: DatabaseModule) {

    enum class VoteResult { VOTED, ALREADY_VOTED, NOT_QUEUED, NOT_FOUND }

    /**
     * Proposes [track] with the proposer's vote. When it already waits in the event, votes for it instead.
     * Returns the song's id and whether it was new.
     */
    suspend fun propose(eventId: UUID, track: TrackResponse, userId: UUID): Pair<UUID, Boolean> = db.query {
        val now = OffsetDateTime.now()
        // ON CONFLICT DO NOTHING: the partial unique index (event, song) not played yet decides between two
        // people proposing the same song at the same time.
        val id = EventTracksTable.insertIgnoreAndGetId {
            it[EventTracksTable.eventId] = eventId
            it[spotifyId] = track.id
            it[EventTracksTable.track] = MusicRoomJson.encodeToString(TrackResponse.serializer(), track)
            it[proposedBy] = userId
            it[votes] = 1
            it[status] = EventTracksTable.QUEUED
            it[createdAt] = now
        }?.value
        if (id != null) {
            EventVotesTable.insertIgnore {
                it[EventVotesTable.eventId] = eventId
                it[eventTrackId] = id
                it[EventVotesTable.userId] = userId
                it[createdAt] = now
            }
            id to true
        } else {
            val existing = EventTracksTable.select(EventTracksTable.id)
                .where {
                    (EventTracksTable.eventId eq eventId) and (EventTracksTable.spotifyId eq track.id) and
                        (EventTracksTable.status neq EventTracksTable.PLAYED)
                }
                .single()[EventTracksTable.id].value
            voteInTransaction(eventId, existing, userId)
            existing to false
        }
    }

    suspend fun vote(eventId: UUID, trackId: UUID, userId: UUID): VoteResult = db.query { voteInTransaction(eventId, trackId, userId) }

    /** Takes the user's vote back. False when they had not voted. */
    suspend fun unvote(eventId: UUID, trackId: UUID, userId: UUID): Boolean = db.query {
        val removed = EventVotesTable.deleteWhere {
            (EventVotesTable.eventId eq eventId) and (EventVotesTable.eventTrackId eq trackId) and (EventVotesTable.userId eq userId)
        } == 1
        if (removed) {
            EventTracksTable.update({ EventTracksTable.id eq trackId }) { it[votes] = votes - 1 }
        }
        removed
    }

    /** Songs not played yet: the queue (most voted first, then the oldest) with the next and playing ones. */
    suspend fun pending(eventId: UUID): List<EventTrack> = db.query {
        EventTracksTable.selectAll()
            .where { (EventTracksTable.eventId eq eventId) and (EventTracksTable.status neq EventTracksTable.PLAYED) }
            .orderBy(EventTracksTable.votes to SortOrder.DESC, EventTracksTable.createdAt to SortOrder.ASC)
            .map { it.toTrack() }
    }

    /** The queued song with the most votes (the oldest among equals): it plays next. */
    suspend fun top(eventId: UUID): EventTrack? = db.query {
        EventTracksTable.selectAll()
            .where { (EventTracksTable.eventId eq eventId) and (EventTracksTable.status eq EventTracksTable.QUEUED) }
            .orderBy(EventTracksTable.votes to SortOrder.DESC, EventTracksTable.createdAt to SortOrder.ASC)
            .limit(1)
            .singleOrNull()?.toTrack()
    }

    suspend fun withStatus(eventId: UUID, status: String): EventTrack? = db.query {
        EventTracksTable.selectAll()
            .where { (EventTracksTable.eventId eq eventId) and (EventTracksTable.status eq status) }
            .orderBy(EventTracksTable.createdAt to SortOrder.DESC)
            .limit(1)
            .singleOrNull()?.toTrack()
    }

    suspend fun setStatus(trackId: UUID, status: String) = db.query {
        EventTracksTable.update({ EventTracksTable.id eq trackId }) {
            it[EventTracksTable.status] = status
            if (status == EventTracksTable.PLAYED) it[playedAt] = OffsetDateTime.now()
        }
    }

    /** Removes a song still waiting in the queue. False when there is none. */
    suspend fun remove(eventId: UUID, trackId: UUID): Boolean = db.query {
        EventTracksTable.deleteWhere {
            (EventTracksTable.id eq trackId) and (EventTracksTable.eventId eq eventId) and (EventTracksTable.status eq EventTracksTable.QUEUED)
        } == 1
    }

    suspend fun find(eventId: UUID, trackId: UUID): EventTrack? = db.query {
        EventTracksTable.selectAll().where { (EventTracksTable.id eq trackId) and (EventTracksTable.eventId eq eventId) }
            .singleOrNull()?.toTrack()
    }

    /** Songs of [userId] waiting in the queue (to limit how many one person proposes). */
    suspend fun queuedBy(eventId: UUID, userId: UUID): Int = db.query {
        EventTracksTable.selectAll()
            .where {
                (EventTracksTable.eventId eq eventId) and (EventTracksTable.proposedBy eq userId) and
                    (EventTracksTable.status eq EventTracksTable.QUEUED)
            }
            .count().toInt()
    }

    /** Who voted for each song not played yet, by song id. */
    suspend fun voters(eventId: UUID): Map<UUID, Set<UUID>> = db.query {
        EventVotesTable.join(EventTracksTable, org.jetbrains.exposed.v1.core.JoinType.INNER, EventVotesTable.eventTrackId, EventTracksTable.id)
            .select(EventVotesTable.eventTrackId, EventVotesTable.userId)
            .where { (EventVotesTable.eventId eq eventId) and (EventTracksTable.status neq EventTracksTable.PLAYED) }
            .groupBy({ it[EventVotesTable.eventTrackId].value }, { it[EventVotesTable.userId].value })
            .mapValues { it.value.toSet() }
    }

    /** Number of queued songs of each event. */
    suspend fun queuedCounts(eventIds: Collection<UUID>): Map<UUID, Int> = if (eventIds.isEmpty()) {
        emptyMap()
    } else {
        db.query {
            val count = EventTracksTable.id.count()
            EventTracksTable.select(EventTracksTable.eventId, count)
                .where { (EventTracksTable.eventId inList eventIds) and (EventTracksTable.status eq EventTracksTable.QUEUED) }
                .groupBy(EventTracksTable.eventId)
                .associate { it[EventTracksTable.eventId].value to it[count].toInt() }
        }
    }

    /** The song playing in each of [eventIds] that has one. */
    suspend fun playingIn(eventIds: Collection<UUID>): Map<UUID, TrackResponse> = if (eventIds.isEmpty()) {
        emptyMap()
    } else {
        db.query {
            EventTracksTable.selectAll()
                .where { (EventTracksTable.eventId inList eventIds) and (EventTracksTable.status eq EventTracksTable.PLAYING) }
                .associate { it[EventTracksTable.eventId].value to it.toTrack().track }
        }
    }

    /**
     * Locks the song's row (SELECT ... FOR UPDATE) so the player cannot pick it in the meantime, then adds the vote
     * if this user has none, and counts it.
     */
    private fun voteInTransaction(eventId: UUID, trackId: UUID, userId: UUID): VoteResult {
        val row = EventTracksTable.selectAll()
            .where { (EventTracksTable.id eq trackId) and (EventTracksTable.eventId eq eventId) }
            .forUpdate()
            .singleOrNull() ?: return VoteResult.NOT_FOUND
        if (row[EventTracksTable.status] != EventTracksTable.QUEUED) return VoteResult.NOT_QUEUED
        val inserted = EventVotesTable.insertIgnore {
            it[EventVotesTable.eventId] = eventId
            it[eventTrackId] = trackId
            it[EventVotesTable.userId] = userId
            it[createdAt] = OffsetDateTime.now()
        }.insertedCount == 1
        if (!inserted) return VoteResult.ALREADY_VOTED
        EventTracksTable.update({ EventTracksTable.id eq trackId }) { it[votes] = votes + 1 }
        return VoteResult.VOTED
    }

    private fun ResultRow.toTrack() = EventTrack(
        id = this[EventTracksTable.id].value,
        eventId = this[EventTracksTable.eventId].value,
        track = MusicRoomJson.decodeFromString(TrackResponse.serializer(), this[EventTracksTable.track]),
        proposedBy = this[EventTracksTable.proposedBy]?.value,
        votes = this[EventTracksTable.votes],
        status = this[EventTracksTable.status],
        createdAt = this[EventTracksTable.createdAt],
    )
}
