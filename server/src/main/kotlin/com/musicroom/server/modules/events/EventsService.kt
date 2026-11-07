package com.musicroom.server.modules.events

import com.musicroom.server.common.exceptions.BadRequestException
import com.musicroom.server.common.exceptions.ConflictException
import com.musicroom.server.common.exceptions.ForbiddenException
import com.musicroom.server.common.exceptions.NotFoundException
import com.musicroom.server.common.exceptions.ValidationException
import com.musicroom.server.modules.events.entities.Event
import com.musicroom.server.modules.events.entities.EventTrack
import com.musicroom.server.modules.events.entities.EventTracksTable
import com.musicroom.server.modules.social.PeopleRepository
import com.musicroom.server.modules.spotify.SpotifyService
import com.musicroom.server.modules.applemusic.AppleMusicCatalog
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.EventValidation
import com.musicroom.shared.GeoDistance
import com.musicroom.shared.dto.CreateEventRequest
import com.musicroom.shared.dto.EventDetailResponse
import com.musicroom.shared.dto.EventResponse
import com.musicroom.shared.dto.EventStatus
import com.musicroom.shared.dto.EventTrackResponse
import com.musicroom.shared.dto.EventTrackStatus
import com.musicroom.shared.dto.EventVisibility
import com.musicroom.shared.dto.EventVoteLicense
import com.musicroom.shared.dto.EventsResponse
import com.musicroom.shared.dto.EventsScope
import com.musicroom.shared.dto.GeoPoint
import com.musicroom.shared.dto.PublicUserResponse
import com.musicroom.shared.dto.TrackResponse
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Events (Music Track Vote): who sees them, who proposes and votes (the vote license), the queue ordered by votes,
 * and the owner's commands. The music itself is played by [player] on the owner's Spotify; [hub] pushes every
 * change to the people following the event.
 *
 * A private event is answered "not found" to people who were not invited, so its existence does not leak.
 */
class EventsService(
    private val events: EventsRepository,
    private val tracks: EventTracksRepository,
    private val people: PeopleRepository,
    private val spotify: SpotifyService,
    private val friends: EventFriends,
    private val appleMusic: AppleMusicCatalog,
) {
    val hub = EventsHub { eventId, userIds -> render(eventId, userIds) }
    val player = EventPlayer(events, tracks, spotify, onChange = hub::changed)

    suspend fun create(userId: UUID, request: CreateEventRequest): EventDetailResponse {
        validate(request)
        val event = events.create(userId, request)
        return detail(userId, event.id)
    }

    suspend fun update(userId: UUID, eventId: UUID, request: CreateEventRequest): EventDetailResponse {
        owned(userId, eventId)
        validate(request)
        events.update(eventId, request)
        hub.changed(eventId)
        return detail(userId, eventId)
    }

    suspend fun delete(userId: UUID, eventId: UUID) {
        val event = owned(userId, eventId)
        if (event.status != EventStatus.WAITING) player.stop(event)
        events.delete(eventId)
        player.forget(eventId)
        hub.deleted(eventId)
    }

    /** Events of [scope]; [near] sorts the nearby ones (required for [EventsScope.NEARBY]). */
    suspend fun list(userId: UUID, scope: EventsScope, near: GeoPoint?): EventsResponse {
        val found = when (scope) {
            EventsScope.PUBLIC -> events.public(LIST_LIMIT).sortedByDescending { it.status == EventStatus.PLAYING }
            EventsScope.NEARBY -> {
                val here = near ?: throw BadRequestException("Send your position to find events nearby", ApiErrors.EVENT_LOCATION_REQUIRED)
                events.public(NEARBY_CANDIDATES, located = true)
                    .map { it to GeoDistance.meters(here, it.location!!) }
                    .filter { (_, meters) -> meters <= NEARBY_METERS }
                    .sortedBy { (_, meters) -> meters }
                    .map { it.first }
                    .take(LIST_LIMIT)
            }
            EventsScope.INVITED -> events.invitedTo(userId)
            EventsScope.MINE -> events.ownedBy(userId)
        }
        return EventsResponse(summaries(userId, found))
    }

    suspend fun detail(userId: UUID, eventId: UUID): EventDetailResponse {
        visible(userId, eventId)
        return render(eventId, setOf(userId)).getValue(userId)
    }

    /** Proposes a Spotify track (with the proposer's vote), or votes for it when it already waits in the queue. */
    suspend fun propose(userId: UUID, eventId: UUID, trackId: String, location: GeoPoint?): EventDetailResponse {
        val event = visible(userId, eventId)
        checkVote(event, userId, location)
        if (event.ownerId != userId && tracks.queuedBy(eventId, userId) >= MAX_QUEUED_PER_USER) {
            throw ConflictException("You already have $MAX_QUEUED_PER_USER songs waiting", ApiErrors.EVENT_PROPOSAL_LIMIT)
        }
        if (trackId.isBlank() || trackId.length > 64) throw BadRequestException("Unknown track")
        // The event plays on Spotify, which cannot play the songs uploaded to Music Room.
        if (trackId.startsWith(TrackResponse.HOSTED_PREFIX)) {
            throw BadRequestException("Songs uploaded to Music Room cannot play in an event", ApiErrors.HOSTED_TRACK_NOT_ALLOWED)
        }
        // Resolve catalogue metadata on the server before accepting a track.
        val track = if (trackId.startsWith("apple:")) appleMusic.track(trackId) else spotify.track(userId, trackId)
        tracks.propose(eventId, track, userId)
        hub.changed(eventId)
        return detail(userId, eventId)
    }

    suspend fun vote(userId: UUID, eventId: UUID, trackId: UUID, location: GeoPoint?): EventDetailResponse {
        val event = visible(userId, eventId)
        checkVote(event, userId, location)
        when (tracks.vote(eventId, trackId, userId)) {
            EventTracksRepository.VoteResult.NOT_FOUND -> throw NotFoundException("No such song in this event")
            EventTracksRepository.VoteResult.VOTED -> hub.changed(eventId)
            // Already voted, or the song already plays: nothing changes.
            else -> Unit
        }
        return detail(userId, eventId)
    }

    suspend fun unvote(userId: UUID, eventId: UUID, trackId: UUID): EventDetailResponse {
        visible(userId, eventId)
        if (tracks.unvote(eventId, trackId, userId)) hub.changed(eventId)
        return detail(userId, eventId)
    }

    /** The owner removes any queued song; the others only their own. */
    suspend fun removeTrack(userId: UUID, eventId: UUID, trackId: UUID): EventDetailResponse {
        val event = visible(userId, eventId)
        val track = tracks.find(eventId, trackId) ?: throw NotFoundException("No such song in this event")
        if (event.ownerId != userId && track.proposedBy != userId) {
            throw ForbiddenException("Only the owner can remove the songs of others")
        }
        if (!tracks.remove(eventId, trackId)) throw ConflictException("This song already plays")
        hub.changed(eventId)
        return detail(userId, eventId)
    }

    /** The owner invites one of their friends, who gets a notification. */
    suspend fun invite(userId: UUID, eventId: UUID, friendId: UUID): EventDetailResponse {
        owned(userId, eventId)
        if (!friends.areFriends(userId, friendId)) throw ForbiddenException("You can only invite your friends", ApiErrors.NOT_FRIENDS)
        if (events.invite(eventId, friendId, userId)) {
            friends.invitedToEvent(eventId, userId, friendId)
            hub.changed(eventId)
        }
        return detail(userId, eventId)
    }

    /** The owner takes an invitation back, or the person invited leaves. */
    suspend fun uninvite(userId: UUID, eventId: UUID, invitedId: UUID) {
        val event = visible(userId, eventId)
        if (event.ownerId != userId && invitedId != userId) throw ForbiddenException("Only the owner can remove people")
        if (events.uninvite(eventId, invitedId)) hub.changed(eventId)
    }

    suspend fun play(userId: UUID, eventId: UUID): EventDetailResponse {
        player.start(owned(userId, eventId))
        return detail(userId, eventId)
    }

    suspend fun pause(userId: UUID, eventId: UUID): EventDetailResponse {
        player.pause(owned(userId, eventId))
        return detail(userId, eventId)
    }

    suspend fun skip(userId: UUID, eventId: UUID): EventDetailResponse {
        player.skip(owned(userId, eventId))
        return detail(userId, eventId)
    }

    suspend fun stop(userId: UUID, eventId: UUID): EventDetailResponse {
        player.stop(owned(userId, eventId))
        return detail(userId, eventId)
    }

    /** The event if [userId] may see it (for the WebSocket); throws "not found" otherwise. */
    suspend fun visible(userId: UUID, eventId: UUID): Event {
        val event = events.find(eventId) ?: throw notFound()
        if (event.visibility == EventVisibility.PRIVATE && event.ownerId != userId && !events.isInvited(eventId, userId)) {
            throw notFound()
        }
        return event
    }

    /**
     * Checks the vote license: everyone; the people invited; or the people within the area during the hours.
     * The owner always can.
     */
    private suspend fun checkVote(event: Event, userId: UUID, location: GeoPoint?) {
        if (event.ownerId == userId) return
        when (event.voteLicense) {
            EventVoteLicense.EVERYONE -> Unit
            EventVoteLicense.INVITED -> if (!events.isInvited(event.id, userId)) {
                throw ForbiddenException("Only the people invited can vote", ApiErrors.EVENT_VOTE_NOT_ALLOWED)
            }
            EventVoteLicense.LOCATION -> {
                if (!isOpen(event, OffsetDateTime.now())) {
                    throw ForbiddenException("Votes are closed at this time", ApiErrors.EVENT_CLOSED)
                }
                val here = location?.takeIf(GeoDistance::isValid)
                    ?: throw BadRequestException("Send your position to vote", ApiErrors.EVENT_LOCATION_REQUIRED)
                val center = event.location ?: throw ForbiddenException("This event has no area", ApiErrors.EVENT_OUT_OF_AREA)
                if (GeoDistance.meters(here, center) > (event.radiusMeters ?: 0)) {
                    throw ForbiddenException("You are too far from the event to vote", ApiErrors.EVENT_OUT_OF_AREA)
                }
            }
        }
    }

    private fun isOpen(event: Event, now: OffsetDateTime): Boolean =
        (event.startsAt == null || !now.isBefore(event.startsAt)) && (event.endsAt == null || !now.isAfter(event.endsAt))

    private suspend fun owned(userId: UUID, eventId: UUID): Event {
        val event = visible(userId, eventId)
        if (event.ownerId != userId) throw ForbiddenException("Only the owner can do this")
        return event
    }

    private fun validate(request: CreateEventRequest) {
        EventValidation.error(request)?.let { throw ValidationException(it) }
    }

    /** The event page as each of [userIds] sees it, read once for all of them. */
    private suspend fun render(eventId: UUID, userIds: Set<UUID>): Map<UUID, EventDetailResponse> {
        val event = events.find(eventId) ?: return emptyMap()
        val pending = tracks.pending(eventId)
        val voters = tracks.voters(eventId)
        val invited = events.invited(eventId)
        val users = people.find((pending.mapNotNull { it.proposedBy } + invited + event.ownerId).toSet())
        val now = OffsetDateTime.now()
        val listeners = hub.listeners(eventId)
        val progress = player.progressMs(event)
        val sentAt = System.currentTimeMillis()
        val invitedUsers = invited.mapNotNull { users[it] }
        return userIds.associateWith { userId ->
            fun view(track: EventTrack) = EventTrackResponse(
                id = track.id.toString(),
                track = track.track,
                proposedBy = track.proposedBy?.let { users[it] },
                votes = track.votes,
                voted = voters[track.id]?.contains(userId) == true,
                status = when (track.status) {
                    EventTracksTable.PLAYING -> EventTrackStatus.PLAYING
                    EventTracksTable.NEXT -> EventTrackStatus.NEXT
                    else -> EventTrackStatus.QUEUED
                },
            )
            val playing = pending.firstOrNull { it.status == EventTracksTable.PLAYING }
            EventDetailResponse(
                event = summary(
                    event,
                    userId,
                    owner = users[event.ownerId],
                    invited = userId in invited,
                    now = now,
                    queuedCount = pending.count { it.status == EventTracksTable.QUEUED },
                    listeners = listeners,
                    nowPlaying = playing?.track,
                ),
                nowPlaying = playing?.let(::view),
                next = pending.firstOrNull { it.status == EventTracksTable.NEXT }?.let(::view),
                queue = pending.filter { it.status == EventTracksTable.QUEUED }.map(::view),
                invited = invitedUsers,
                playbackIssue = if (userId == event.ownerId) player.issue(eventId) else null,
                progressMs = progress,
                sentAt = sentAt,
            )
        }
    }

    private suspend fun summaries(userId: UUID, found: List<Event>): List<EventResponse> {
        val ids = found.map { it.id }
        val invited = events.invitedAmong(userId, ids)
        val owners = people.find(found.map { it.ownerId }.toSet())
        val counts = tracks.queuedCounts(ids)
        val playing = tracks.playingIn(ids)
        val now = OffsetDateTime.now()
        return found.map { event ->
            summary(
                event,
                userId,
                owner = owners[event.ownerId],
                invited = event.id in invited,
                now = now,
                queuedCount = counts[event.id] ?: 0,
                listeners = hub.listeners(event.id),
                nowPlaying = playing[event.id],
            )
        }
    }

    private fun summary(
        event: Event,
        userId: UUID,
        owner: PublicUserResponse?,
        invited: Boolean,
        now: OffsetDateTime,
        queuedCount: Int,
        listeners: Int,
        nowPlaying: com.musicroom.shared.dto.TrackResponse?,
    ): EventResponse {
        val isOwner = event.ownerId == userId
        val canVote = isOwner || when (event.voteLicense) {
            EventVoteLicense.EVERYONE -> true
            EventVoteLicense.INVITED -> invited
            EventVoteLicense.LOCATION -> isOpen(event, now)
        }
        return EventResponse(
            id = event.id.toString(),
            name = event.name,
            description = event.description,
            visibility = event.visibility,
            voteLicense = event.voteLicense,
            status = event.status,
            owner = owner ?: PublicUserResponse(event.ownerId.toString(), ""),
            isOwner = isOwner,
            isInvited = invited,
            canVote = canVote,
            location = event.location,
            radiusMeters = event.radiusMeters,
            startsAt = event.startsAt?.toInstant()?.toEpochMilli(),
            endsAt = event.endsAt?.toInstant()?.toEpochMilli(),
            queuedCount = queuedCount,
            listeners = listeners,
            nowPlaying = nowPlaying,
        )
    }

    private fun notFound() = NotFoundException("No such event")

    private companion object {
        const val LIST_LIMIT = 50
        const val NEARBY_CANDIDATES = 500
        const val NEARBY_METERS = 50_000.0

        /** Songs one person may have waiting at once (spam protection; the owner has no limit). */
        const val MAX_QUEUED_PER_USER = 5
    }
}
