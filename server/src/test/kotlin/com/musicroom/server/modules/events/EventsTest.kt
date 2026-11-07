package com.musicroom.server.modules.events

import com.musicroom.server.AppModule
import com.musicroom.server.bootstrap
import com.musicroom.server.common.security.PasswordHasher
import com.musicroom.server.config.AppConfig
import com.musicroom.server.config.DatabaseConfig
import com.musicroom.server.config.JwtConfig
import com.musicroom.server.config.SpotifyConfig
import com.musicroom.server.database.TestDatabase
import com.musicroom.server.modules.spotify.FakeSpotifyApi
import com.musicroom.server.modules.spotify.client.model.SpotifyDevice
import com.musicroom.server.modules.spotify.client.model.SpotifyPlaybackState
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.ApiRoutes
import com.musicroom.shared.MusicRoomJson
import com.musicroom.shared.dto.AuthResponse
import com.musicroom.shared.dto.CreateEventRequest
import com.musicroom.shared.dto.ErrorResponse
import com.musicroom.shared.dto.EventDetailResponse
import com.musicroom.shared.dto.EventLiveEvent
import com.musicroom.shared.dto.EventStatus
import com.musicroom.shared.dto.EventTrackStatus
import com.musicroom.shared.dto.EventVisibility
import com.musicroom.shared.dto.EventVoteLicense
import com.musicroom.shared.dto.EventVoteRequest
import com.musicroom.shared.dto.EventsResponse
import com.musicroom.shared.dto.GeoPoint
import com.musicroom.shared.dto.NotificationType
import com.musicroom.shared.dto.NotificationsResponse
import com.musicroom.shared.dto.ProposeTrackRequest
import com.musicroom.shared.dto.RegisterRequest
import com.musicroom.shared.dto.SpotifyConnectResponse
import com.musicroom.shared.dto.TrackResponse
import com.musicroom.shared.dto.TrackSource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers(disabledWithoutDocker = true)
class EventsTest {
    @Test
    fun `apple track uses the clock even when the owner has spotify premium`() = testApplication {
        val client = client()
        val owner = client.user(spotify = true)
        val event = client.create(owner).event
        val apple = TrackResponse(
            id = "apple:us:54321", name = "Apple track", artists = listOf("Artist"),
            album = "Album", durationMs = 200_000, uri = "54321", source = TrackSource.APPLE_MUSIC,
        )
        EventTracksRepository(TestDatabase.module).propose(UUID.fromString(event.id), apple, UUID.fromString(owner.id))
        spotify.plays.clear()
        val started: EventDetailResponse = client.json(HttpMethodCall.POST, ApiRoutes.eventPlay(event.id), owner).body()
        assertEquals(apple.id, started.nowPlaying?.track?.id)
        appModule.events.service.player.tick(UUID.fromString(event.id))
        assertTrue(spotify.plays.isEmpty())
        assertEquals(apple.id, client.detail(owner, event.id).nowPlaying?.track?.id)
    }


    @Test
    fun `apple track is never queued on spotify and uses the event clock`() = testApplication {
        val client = client()
        val owner = client.user(spotify = true)
        val event = client.create(owner).event
        client.propose(owner, event.id, "t1")
        client.json(HttpMethodCall.POST, ApiRoutes.eventPlay(event.id), owner)
        val apple = TrackResponse(
            id = "apple:us:12345", name = "Apple track", artists = listOf("Artist"),
            album = "Album", durationMs = 200_000, uri = "12345", source = TrackSource.APPLE_MUSIC,
        )
        EventTracksRepository(TestDatabase.module).propose(UUID.fromString(event.id), apple, UUID.fromString(owner.id))
        spotify.queued.clear()
        val pausesBefore = spotify.pauses
        spotify.playbackState = SpotifyPlaybackState(isPlaying = true, progressMs = 190_000, item = spotify.tracks.getValue("t1"))
        appModule.events.service.player.tick(UUID.fromString(event.id))
        assertTrue(spotify.queued.isEmpty())
        assertEquals(apple.id, client.detail(owner, event.id).queue.first().track.id)

        spotify.playbackState = SpotifyPlaybackState(isPlaying = false, progressMs = 0, item = spotify.tracks.getValue("t1"))
        appModule.events.service.player.tick(UUID.fromString(event.id))
        assertEquals(apple.id, client.detail(owner, event.id).nowPlaying?.track?.id)
        assertTrue(spotify.pauses > pausesBefore)
        assertTrue(spotify.plays.none { it.second?.uris?.contains(apple.uri) == true })
    }

    private val spotify = FakeSpotifyApi().apply {
        tracks = listOf("t1", "t2", "t3").associateWith { FakeSpotifyApi.track(it, "Daft Punk").copy(durationMs = 200_000) }
        devices = listOf(SpotifyDevice(id = "phone", isActive = true, name = "Phone", type = "Smartphone"))
    }
    private val config = AppConfig(
        version = "test",
        database = DatabaseConfig("unused", "unused", "unused"),
        jwt = JwtConfig(secret = "x".repeat(32), issuer = "test", audience = "test", realm = "test"),
        spotify = SpotifyConfig(clientId = "id", clientSecret = "secret"),
    )
    private val appModule = AppModule(config, TestDatabase.module, PasswordHasher(cost = 4), emailSender = { _, _, _ -> }, spotifyApi = spotify)
    private val paris = GeoPoint(48.8566, 2.3522)

    private class TestUser(val id: String, val token: String)

    private fun ApplicationTestBuilder.client(): HttpClient {
        environment { config = MapApplicationConfig() }
        application { bootstrap(appModule) }
        return createClient { install(ContentNegotiation) { json(MusicRoomJson) } }
    }

    /** Signs up; [spotify]: also links Spotify (only the event owner needs it, to play the music). */
    private suspend fun HttpClient.user(spotify: Boolean = false): TestUser {
        val auth: AuthResponse = post(ApiRoutes.AUTH_REGISTER) {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("user-${UUID.randomUUID()}@example.com", "hunter42abc", "Sam ${UUID.randomUUID().toString().take(8)}", acceptedTerms = true))
        }.body()
        if (spotify) {
            val connect: SpotifyConnectResponse = post(ApiRoutes.SPOTIFY_CONNECT) { bearerAuth(auth.accessToken) }.body()
            get(ApiRoutes.SPOTIFY_CALLBACK) {
                parameter("code", "code")
                parameter("state", Url(connect.authorizeUrl).parameters["state"])
            }
        }
        return TestUser(auth.user.id, auth.accessToken)
    }

    private suspend fun HttpClient.json(method: HttpMethodCall, path: String, user: TestUser, body: Any? = null): HttpResponse =
        method(this, path) {
            bearerAuth(user.token)
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }

    private suspend fun HttpClient.create(owner: TestUser, request: CreateEventRequest = CreateEventRequest("Rooftop party")): EventDetailResponse =
        json(HttpMethodCall.POST, ApiRoutes.EVENTS, owner, request).body()

    private suspend fun HttpClient.propose(user: TestUser, eventId: String, trackId: String, location: GeoPoint? = null) =
        json(HttpMethodCall.POST, ApiRoutes.eventTracks(eventId), user, ProposeTrackRequest(trackId, location))

    private suspend fun HttpClient.vote(user: TestUser, eventId: String, trackId: String, location: GeoPoint? = null) =
        json(HttpMethodCall.POST, ApiRoutes.eventTrackVote(eventId, trackId), user, EventVoteRequest(location))

    private suspend fun HttpClient.detail(user: TestUser, eventId: String): EventDetailResponse =
        json(HttpMethodCall.GET, ApiRoutes.event(eventId), user).body()

    private suspend fun HttpClient.befriend(a: TestUser, b: TestUser) {
        json(HttpMethodCall.POST, ApiRoutes.friend(b.id), a)
        json(HttpMethodCall.POST, ApiRoutes.friend(a.id), b)
    }

    @Test
    fun `people without Spotify propose and vote, the most voted song comes first`() = testApplication {
        val client = client()
        val owner = client.user()
        val sam = client.user()
        val lea = client.user()
        val event = client.create(owner).event

        // Listed in the public events.
        val listed: EventsResponse = client.json(HttpMethodCall.GET, ApiRoutes.EVENTS, sam).body()
        assertTrue(listed.items.any { it.id == event.id })

        // Proposing counts the proposer's vote; the details come from the Spotify catalog (app token).
        val first: EventDetailResponse = client.propose(sam, event.id, "t1").body()
        assertEquals(listOf("t1" to 1), first.queue.map { it.track.id to it.votes })
        assertTrue(first.queue.single().voted)
        val second: EventDetailResponse = client.propose(lea, event.id, "t2").body()
        val t2 = second.queue.first { it.track.id == "t2" }.id

        // Two votes for t2: it goes first. A second vote of the same person does not count.
        client.vote(sam, event.id, t2)
        client.vote(sam, event.id, t2)
        val ordered = client.detail(owner, event.id)
        assertEquals(listOf("t2" to 2, "t1" to 1), ordered.queue.map { it.track.id to it.votes })

        // Proposing a song already waiting is a vote for it.
        val again: EventDetailResponse = client.propose(owner, event.id, "t1").body()
        assertEquals(2, again.queue.first { it.track.id == "t1" }.votes)
        assertEquals(1, again.queue.count { it.track.id == "t1" })

        // Taking a vote back.
        val withdrawn: EventDetailResponse = client.json(HttpMethodCall.DELETE, ApiRoutes.eventTrackVote(event.id, t2), sam).body()
        assertEquals(1, withdrawn.queue.first { it.id == t2 }.votes)
        assertTrue(!withdrawn.queue.first { it.id == t2 }.voted)
    }

    @Test
    fun `votes sent at the same time are all counted once`() = testApplication {
        val client = client()
        val owner = client.user()
        val event = client.create(owner).event
        val trackId = client.propose(owner, event.id, "t1").body<EventDetailResponse>().queue.single().id
        // Created directly: sign-ups are rate limited.
        val voters = List(12) {
            val user = appModule.users.repository.create("voter-${UUID.randomUUID()}@example.com", "Voter ${UUID.randomUUID()}", null)
            TestUser(user.id.toString(), appModule.jwtService.createAccessToken(user.id))
        }

        coroutineScope {
            // Each person votes twice at the same time as the others.
            (voters + voters).map { async { client.vote(it, event.id, trackId) } }.awaitAll()
        }

        assertEquals(1 + voters.size, client.detail(owner, event.id).queue.single().votes)
    }

    @Test
    fun `a private event is hidden from people not invited, and invitations go to friends`() = testApplication {
        val client = client()
        val owner = client.user()
        val friend = client.user()
        val stranger = client.user()
        val event = client.create(owner, CreateEventRequest("Secret", visibility = EventVisibility.PRIVATE)).event

        assertEquals(HttpStatusCode.NotFound, client.json(HttpMethodCall.GET, ApiRoutes.event(event.id), stranger).status)
        val publicList: EventsResponse = client.json(HttpMethodCall.GET, ApiRoutes.EVENTS, stranger).body()
        assertTrue(publicList.items.none { it.id == event.id })

        val refused = client.json(HttpMethodCall.POST, ApiRoutes.eventInvited(event.id, stranger.id), owner)
        assertEquals(ApiErrors.NOT_FRIENDS, refused.body<ErrorResponse>().error)

        client.befriend(owner, friend)
        assertEquals(HttpStatusCode.OK, client.json(HttpMethodCall.POST, ApiRoutes.eventInvited(event.id, friend.id), owner).status)
        assertTrue(client.detail(friend, event.id).event.isInvited)
        val invited: EventsResponse = client.json(HttpMethodCall.GET, "${ApiRoutes.EVENTS}?scope=invited", friend).body()
        assertEquals(listOf(event.id), invited.items.map { it.id })
        val notifications: NotificationsResponse = client.json(HttpMethodCall.GET, ApiRoutes.NOTIFICATIONS, friend).body()
        val invitation = notifications.items.first { it.type == NotificationType.EVENT_INVITE }
        assertEquals("Secret", invitation.eventName)
    }

    @Test
    fun `the vote license limits who proposes and votes`() = testApplication {
        val client = client()
        val owner = client.user()
        val guest = client.user()
        val other = client.user()
        client.befriend(owner, guest)

        val invitedOnly = client.create(owner, CreateEventRequest("Guests", voteLicense = EventVoteLicense.INVITED)).event
        client.json(HttpMethodCall.POST, ApiRoutes.eventInvited(invitedOnly.id, guest.id), owner)
        assertEquals(HttpStatusCode.OK, client.propose(guest, invitedOnly.id, "t1").status)
        val refused = client.propose(other, invitedOnly.id, "t2")
        assertEquals(HttpStatusCode.Forbidden, refused.status)
        assertEquals(ApiErrors.EVENT_VOTE_NOT_ALLOWED, refused.body<ErrorResponse>().error)
        // They can still follow it.
        assertTrue(!client.detail(other, invitedOnly.id).event.canVote)

        val now = System.currentTimeMillis()
        val area = CreateEventRequest(
            "Park",
            voteLicense = EventVoteLicense.LOCATION,
            location = paris,
            radiusMeters = 200,
            startsAt = now - 60_000,
            endsAt = now + 3_600_000,
        )
        val local = client.create(owner, area).event
        assertEquals(ApiErrors.EVENT_LOCATION_REQUIRED, client.propose(other, local.id, "t1").body<ErrorResponse>().error)
        val far = GeoPoint(45.7640, 4.8357)
        assertEquals(ApiErrors.EVENT_OUT_OF_AREA, client.propose(other, local.id, "t1", far).body<ErrorResponse>().error)
        // About 100 m away: inside.
        val near = GeoPoint(paris.latitude + 0.0009, paris.longitude)
        assertEquals(HttpStatusCode.OK, client.propose(other, local.id, "t1", near).status)

        val closed = client.create(owner, area.copy(startsAt = now - 7_200_000, endsAt = now - 3_600_000)).event
        assertEquals(ApiErrors.EVENT_CLOSED, client.propose(other, closed.id, "t1", near).body<ErrorResponse>().error)

        // Nearby lists it for someone close, not for someone far.
        val nearby: EventsResponse = client.json(
            HttpMethodCall.GET, "${ApiRoutes.EVENTS}?scope=nearby&lat=${near.latitude}&lng=${near.longitude}", other,
        ).body()
        assertTrue(nearby.items.any { it.id == local.id })
        val fromLyon: EventsResponse = client.json(
            HttpMethodCall.GET, "${ApiRoutes.EVENTS}?scope=nearby&lat=${far.latitude}&lng=${far.longitude}", other,
        ).body()
        assertTrue(fromLyon.items.none { it.id == local.id })
    }

    @Test
    fun `only the owner streams - the player plays the most voted song, then hands the next one to Spotify`() = testApplication {
        val client = client()
        val owner = client.user(spotify = true)
        val guest = client.user()
        val event = client.create(owner).event
        client.propose(guest, event.id, "t1")
        val t2 = client.propose(guest, event.id, "t2").body<EventDetailResponse>().queue.first { it.track.id == "t2" }.id
        client.vote(owner, event.id, t2)
        spotify.plays.clear()
        spotify.queued.clear()

        // The guest cannot start it; the owner's Spotify plays the most voted song.
        assertEquals(HttpStatusCode.Forbidden, client.json(HttpMethodCall.POST, ApiRoutes.eventPlay(event.id), guest).status)
        val started: EventDetailResponse = client.json(HttpMethodCall.POST, ApiRoutes.eventPlay(event.id), owner).body()
        assertEquals(EventStatus.PLAYING, started.event.status)
        assertEquals("t2", started.nowPlaying?.track?.id)
        assertEquals(listOf("spotify:track:t2"), spotify.plays.last().second?.uris)

        // Near the end of t2, the next most voted song is handed to Spotify's queue.
        val player = appModule.events.service.player
        val eventId = UUID.fromString(event.id)
        spotify.playbackState = SpotifyPlaybackState(isPlaying = true, progressMs = 190_000, item = spotify.tracks.getValue("t2"))
        player.tick(eventId)
        assertEquals("spotify:track:t1", spotify.queued.last().second)
        assertEquals(EventTrackStatus.NEXT, client.detail(guest, event.id).next?.status)

        // Spotify moves on to t1: it plays, the queue is empty.
        spotify.playbackState = SpotifyPlaybackState(isPlaying = true, progressMs = 1_000, item = spotify.tracks.getValue("t1"))
        player.tick(eventId)
        val moved = client.detail(guest, event.id)
        assertEquals("t1", moved.nowPlaying?.track?.id)
        assertNull(moved.next)
        assertTrue(moved.queue.isEmpty())

        // t1 ends with nothing queued; a new proposal starts right away at the next check.
        spotify.playbackState = SpotifyPlaybackState(isPlaying = false, progressMs = 0, item = spotify.tracks.getValue("t1"))
        client.propose(guest, event.id, "t3")
        player.tick(eventId)
        assertEquals("t3", client.detail(guest, event.id).nowPlaying?.track?.id)
        assertEquals(listOf("spotify:track:t3"), spotify.plays.last().second?.uris)

        val stopped: EventDetailResponse = client.json(HttpMethodCall.POST, ApiRoutes.eventStop(event.id), owner).body()
        assertEquals(EventStatus.WAITING, stopped.event.status)
        assertNull(stopped.nowPlaying)
    }

    @Test
    fun `without the owner's Spotify Premium, the event plays on a clock`() = testApplication {
        val client = client()
        val owner = client.user()
        val event = client.create(owner).event
        spotify.tracks = spotify.tracks + ("short" to FakeSpotifyApi.track("short", "Daft Punk").copy(durationMs = 50))
        client.propose(owner, event.id, "short")
        spotify.plays.clear()

        // Starting does not ask for Spotify: the song "plays" for its length, nothing is sent to Spotify.
        val started = client.json(HttpMethodCall.POST, ApiRoutes.eventPlay(event.id), owner)
        assertEquals(HttpStatusCode.OK, started.status)
        val detail = started.body<EventDetailResponse>()
        assertEquals(EventStatus.PLAYING, detail.event.status)
        assertEquals("short", detail.nowPlaying?.track?.id)
        assertTrue(spotify.plays.isEmpty())

        // Once its length has passed, the next most voted song follows.
        client.propose(owner, event.id, "t1")
        Thread.sleep(100)
        appModule.events.service.player.tick(UUID.fromString(event.id))
        assertEquals("t1", client.detail(owner, event.id).nowPlaying?.track?.id)
        assertTrue(spotify.plays.isEmpty())

        val paused: EventDetailResponse = client.json(HttpMethodCall.POST, ApiRoutes.eventPause(event.id), owner).body()
        assertEquals(EventStatus.PAUSED, paused.event.status)
    }

    @Test
    fun `followers get the new state live`() = testApplication {
        val client = client()
        val owner = client.user()
        val guest = client.user()
        val event = client.create(owner).event
        val ws = createClient { install(WebSockets) }

        ws.webSocket("${ApiRoutes.eventWs(event.id)}?token=${owner.token}") {
            suspend fun next(): EventDetailResponse {
                val live = MusicRoomJson.decodeFromString(EventLiveEvent.serializer(), (incoming.receive() as Frame.Text).readText())
                return (live as EventLiveEvent.State).detail
            }
            assertEquals(1, withTimeout(5_000) { next() }.event.listeners)
            client.propose(guest, event.id, "t1")
            var state = withTimeout(5_000) { next() }
            while (state.queue.isEmpty()) state = withTimeout(5_000) { next() }
            assertEquals("t1", state.queue.single().track.id)
            assertTrue(!state.queue.single().voted)
        }
    }

    @Test
    fun `the owner edits and deletes the event, others cannot`() = testApplication {
        val client = client()
        val owner = client.user()
        val other = client.user()
        val event = client.create(owner).event
        val request = CreateEventRequest("Renamed", visibility = EventVisibility.PRIVATE)
        assertEquals(HttpStatusCode.Forbidden, client.json(HttpMethodCall.PUT, ApiRoutes.event(event.id), other, request).status)
        assertEquals("Renamed", client.json(HttpMethodCall.PUT, ApiRoutes.event(event.id), owner, request).body<EventDetailResponse>().event.name)
        assertEquals(HttpStatusCode.BadRequest, client.json(HttpMethodCall.PUT, ApiRoutes.event(event.id), owner, request.copy(name = " ")).status)
        assertEquals(HttpStatusCode.NoContent, client.json(HttpMethodCall.DELETE, ApiRoutes.event(event.id), owner).status)
        assertEquals(HttpStatusCode.NotFound, client.json(HttpMethodCall.GET, ApiRoutes.event(event.id), owner).status)
    }

    private enum class HttpMethodCall {
        GET, POST, PUT, DELETE;

        suspend operator fun invoke(
            client: HttpClient,
            path: String,
            block: io.ktor.client.request.HttpRequestBuilder.() -> Unit,
        ): HttpResponse = when (this) {
            GET -> client.get(path, block)
            POST -> client.post(path, block)
            PUT -> client.put(path, block)
            DELETE -> client.delete(path, block)
        }
    }
}
