package com.musicroom.server.modules.playlists

import com.musicroom.server.AppModule
import com.musicroom.server.bootstrap
import com.musicroom.server.common.security.PasswordHasher
import com.musicroom.server.config.AppConfig
import com.musicroom.server.config.DatabaseConfig
import com.musicroom.server.config.JwtConfig
import com.musicroom.server.config.SpotifyConfig
import com.musicroom.server.database.TestDatabase
import com.musicroom.server.modules.spotify.FakeSpotifyApi
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.ApiRoutes
import com.musicroom.shared.MusicRoomJson
import com.musicroom.shared.ValidationError
import com.musicroom.shared.dto.AddPlaylistMemberRequest
import com.musicroom.shared.dto.AddPlaylistTrackRequest
import com.musicroom.shared.dto.AuthResponse
import com.musicroom.shared.dto.CreatePlaylistRequest
import com.musicroom.shared.dto.ErrorResponse
import com.musicroom.shared.dto.PlaylistAccess
import com.musicroom.shared.dto.PlaylistDetailResponse
import com.musicroom.shared.dto.PlaylistListResponse
import com.musicroom.shared.dto.PlaylistResponse
import com.musicroom.shared.dto.PlaylistType
import com.musicroom.shared.dto.RegisterRequest
import com.musicroom.shared.dto.SpotifyConnectResponse
import com.musicroom.shared.dto.UpdatePlaylistMemberRequest
import com.musicroom.shared.dto.UpdatePlaylistRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
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
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers(disabledWithoutDocker = true)
class PlaylistsTest {

    private val spotify = FakeSpotifyApi().apply {
        tracks = listOf("t1", "t2").associateWith { FakeSpotifyApi.track(it, "Daft Punk") }
    }
    private val config = AppConfig(
        version = "test",
        database = DatabaseConfig("unused", "unused", "unused"),
        jwt = JwtConfig(secret = "x".repeat(32), issuer = "test", audience = "test", realm = "test"),
        spotify = SpotifyConfig(clientId = "id", clientSecret = "secret"),
    )
    private val appModule = AppModule(config, TestDatabase.module, PasswordHasher(cost = 4), emailSender = { _, _, _ -> }, spotifyApi = spotify)

    private fun ApplicationTestBuilder.client(): HttpClient {
        environment { config = MapApplicationConfig() }
        application { bootstrap(appModule) }
        return createClient { install(ContentNegotiation) { json(MusicRoomJson) } }
    }

    private class TestUser(val token: String, val email: String)

    /** Signs up and links Spotify (needed to add songs). */
    private suspend fun HttpClient.user(): TestUser {
        val email = "user-${UUID.randomUUID()}@example.com"
        val auth: AuthResponse = post(ApiRoutes.AUTH_REGISTER) {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email, "hunter42abc", "Alice ${UUID.randomUUID().toString().take(8)}", acceptedTerms = true))
        }.body()
        val connect: SpotifyConnectResponse = post(ApiRoutes.SPOTIFY_CONNECT) { bearerAuth(auth.accessToken) }.body()
        get(ApiRoutes.SPOTIFY_CALLBACK) {
            parameter("code", "code")
            parameter("state", Url(connect.authorizeUrl).parameters["state"])
        }
        return TestUser(auth.accessToken, email)
    }

    private suspend inline fun <reified T : Any> HttpClient.send(
        method: suspend HttpClient.(String, io.ktor.client.request.HttpRequestBuilder.() -> Unit) -> HttpResponse,
        path: String,
        token: String,
        body: T? = null,
    ): HttpResponse = method(path) {
        bearerAuth(token)
        if (body != null) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    private suspend fun HttpClient.create(owner: TestUser, type: PlaylistType = PlaylistType.PERSONAL): PlaylistResponse =
        send(HttpClient::post, ApiRoutes.PLAYLISTS, owner.token, CreatePlaylistRequest("  Road trip ", type)).body()

    private suspend fun HttpClient.addTrack(user: TestUser, playlistId: String, trackId: String) =
        send(HttpClient::post, ApiRoutes.roomPlaylistTracks(playlistId), user.token, AddPlaylistTrackRequest(trackId))

    private suspend fun HttpClient.addMember(owner: TestUser, playlistId: String, member: TestUser, access: PlaylistAccess) =
        send(HttpClient::post, ApiRoutes.playlistMembers(playlistId), owner.token, AddPlaylistMemberRequest(member.email, access))

    @Test
    fun `the owner creates a playlist, adds songs in order and removes one`() = testApplication {
        val client = client()
        val owner = client.user()
        val created = client.create(owner)
        assertEquals("Road trip", created.name)
        assertTrue(created.isOwner && created.canEdit)

        client.addTrack(owner, created.id, "t1")
        val detail: PlaylistDetailResponse = client.addTrack(owner, created.id, "t2").body()
        assertEquals(listOf("t1", "t2"), detail.tracks.map { it.track.id })
        assertEquals(detail.playlist.owner.displayName, detail.tracks.first().addedBy)
        assertEquals(detail.playlist.owner.userId, detail.tracks.first().addedById)

        val again = client.addTrack(owner, created.id, "t1")
        assertEquals(HttpStatusCode.Conflict, again.status)
        assertEquals(ApiErrors.TRACK_ALREADY_IN_PLAYLIST, again.body<ErrorResponse>().error)
        assertEquals(HttpStatusCode.NotFound, client.addTrack(owner, created.id, "missing").status)

        val removed: PlaylistDetailResponse =
            client.send<Unit>(HttpClient::delete, ApiRoutes.roomPlaylistTrack(created.id, detail.tracks.first().entryId), owner.token).body()
        assertEquals(listOf("t2"), removed.tracks.map { it.track.id })

        val list: PlaylistListResponse = client.send<Unit>(HttpClient::get, ApiRoutes.PLAYLISTS, owner.token).body()
        assertEquals(1, list.items.single { it.id == created.id }.trackCount)
    }

    @Test
    fun `a blank name is refused`() = testApplication {
        val client = client()
        val owner = client.user()
        val response = client.send(HttpClient::post, ApiRoutes.PLAYLISTS, owner.token, CreatePlaylistRequest("  "))
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(ValidationError.PLAYLIST_NAME_REQUIRED.code, response.body<ErrorResponse>().error)
    }

    @Test
    fun `strangers cannot see a playlist, readers listen, writers edit only a shared playlist`() = testApplication {
        val client = client()
        val owner = client.user()
        val stranger = client.user()
        val member = client.user()
        val playlist = client.create(owner)

        assertEquals(HttpStatusCode.NotFound, client.send<Unit>(HttpClient::get, ApiRoutes.playlist(playlist.id), stranger.token).status)
        assertEquals(HttpStatusCode.NotFound, client.addTrack(stranger, playlist.id, "t1").status)

        // A personal playlist only takes listeners.
        val refused = client.addMember(owner, playlist.id, member, PlaylistAccess.WRITE)
        assertEquals(ApiErrors.PERSONAL_PLAYLIST_READ_ONLY, refused.body<ErrorResponse>().error)
        assertEquals(HttpStatusCode.OK, client.addMember(owner, playlist.id, member, PlaylistAccess.READ).status)
        assertEquals(ApiErrors.ALREADY_MEMBER, client.addMember(owner, playlist.id, member, PlaylistAccess.READ).body<ErrorResponse>().error)

        val seen: PlaylistDetailResponse = client.send<Unit>(HttpClient::get, ApiRoutes.playlist(playlist.id), member.token).body()
        assertFalse(seen.playlist.canEdit)
        assertEquals(HttpStatusCode.Forbidden, client.addTrack(member, playlist.id, "t1").status)

        // Shared + write access: the member edits.
        client.send(HttpClient::patch, ApiRoutes.playlist(playlist.id), owner.token, UpdatePlaylistRequest(type = PlaylistType.SHARED))
        val memberId = seen.members.single().userId
        client.send(HttpClient::patch, ApiRoutes.playlistMember(playlist.id, memberId), owner.token, UpdatePlaylistMemberRequest(PlaylistAccess.WRITE))
        assertEquals(HttpStatusCode.OK, client.addTrack(member, playlist.id, "t1").status)
        val memberList: PlaylistListResponse = client.send<Unit>(HttpClient::get, ApiRoutes.PLAYLISTS, member.token).body()
        assertTrue(memberList.items.single { it.id == playlist.id }.canEdit)

        // Only the owner manages the playlist.
        assertEquals(HttpStatusCode.Forbidden, client.send<Unit>(HttpClient::delete, ApiRoutes.playlist(playlist.id), member.token).status)

        // Back to personal: every member becomes read-only.
        val personal: PlaylistDetailResponse =
            client.send(HttpClient::patch, ApiRoutes.playlist(playlist.id), owner.token, UpdatePlaylistRequest(type = PlaylistType.PERSONAL)).body()
        assertEquals(PlaylistAccess.READ, personal.members.single().access)
        assertEquals(HttpStatusCode.Forbidden, client.addTrack(member, playlist.id, "t2").status)

        // A member can leave.
        assertEquals(HttpStatusCode.NoContent, client.send<Unit>(HttpClient::delete, ApiRoutes.playlistMember(playlist.id, memberId), member.token).status)
        assertEquals(HttpStatusCode.NotFound, client.send<Unit>(HttpClient::get, ApiRoutes.playlist(playlist.id), member.token).status)
    }

    @Test
    fun `the photo is stored as a 512 square JPEG and can be removed`() = testApplication {
        val client = client()
        val owner = client.user()
        val playlist = client.create(owner)
        assertNull(playlist.coverVersion)

        val png = ByteArrayOutputStream().also { ImageIO.write(BufferedImage(900, 600, BufferedImage.TYPE_INT_RGB), "png", it) }.toByteArray()
        val uploaded: PlaylistDetailResponse = client.put(ApiRoutes.playlistCover(playlist.id)) {
            bearerAuth(owner.token)
            contentType(ContentType.Image.PNG)
            setBody(png)
        }.body()
        assertNotNull(uploaded.playlist.coverVersion)

        val bytes: ByteArray = client.send<Unit>(HttpClient::get, ApiRoutes.playlistCover(playlist.id), owner.token).body()
        val image = ImageIO.read(ByteArrayInputStream(bytes))
        assertEquals(512 to 512, image.width to image.height)

        val removed: PlaylistDetailResponse = client.send<Unit>(HttpClient::delete, ApiRoutes.playlistCover(playlist.id), owner.token).body()
        assertNull(removed.playlist.coverVersion)
    }
}
