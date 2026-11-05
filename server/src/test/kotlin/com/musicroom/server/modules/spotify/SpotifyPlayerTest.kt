package com.musicroom.server.modules.spotify

import com.musicroom.server.AppModule
import com.musicroom.server.bootstrap
import com.musicroom.server.common.security.PasswordHasher
import com.musicroom.server.config.AppConfig
import com.musicroom.server.config.DatabaseConfig
import com.musicroom.server.config.JwtConfig
import com.musicroom.server.config.SpotifyConfig
import com.musicroom.server.database.TestDatabase
import com.musicroom.server.modules.spotify.client.SpotifyApiException
import com.musicroom.server.modules.spotify.client.model.SpotifyAlbum
import com.musicroom.server.modules.spotify.client.model.SpotifyArtist
import com.musicroom.server.modules.spotify.client.model.SpotifyDevice
import com.musicroom.server.modules.spotify.client.model.SpotifyImage
import com.musicroom.server.modules.spotify.client.model.SpotifyPaging
import com.musicroom.server.modules.spotify.client.model.SpotifyPlayBody
import com.musicroom.server.modules.spotify.client.model.SpotifyPlaybackState
import com.musicroom.server.modules.spotify.client.model.SpotifyPlaylist
import com.musicroom.server.modules.spotify.client.model.SpotifyTrack
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.ApiRoutes
import com.musicroom.shared.MusicRoomJson
import com.musicroom.shared.dto.AlbumDetailResponse
import com.musicroom.shared.dto.AlbumType
import com.musicroom.shared.dto.ArtistDetailResponse
import com.musicroom.shared.dto.AuthResponse
import com.musicroom.shared.dto.DeviceType
import com.musicroom.shared.dto.DevicesResponse
import com.musicroom.shared.dto.ErrorResponse
import com.musicroom.shared.dto.PlayRequest
import com.musicroom.shared.dto.PlaybackStateResponse
import com.musicroom.shared.dto.PlaylistsResponse
import com.musicroom.shared.dto.QueueRequest
import com.musicroom.shared.dto.QueueResponse
import com.musicroom.shared.dto.RecommendationKind
import com.musicroom.shared.dto.RecommendationsResponse
import com.musicroom.shared.dto.RegisterRequest
import com.musicroom.shared.dto.SearchResponse
import com.musicroom.shared.dto.SpotifyConnectResponse
import com.musicroom.shared.dto.TrackPageResponse
import com.musicroom.shared.dto.TransferRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Testcontainers(disabledWithoutDocker = true)
class SpotifyPlayerTest {

    private val spotify = FakeSpotifyApi()
    private val config = AppConfig(
        version = "test",
        database = DatabaseConfig("unused", "unused", "unused"),
        jwt = JwtConfig(secret = "x".repeat(32), issuer = "test", audience = "test", realm = "test"),
        spotify = SpotifyConfig(clientId = "id", clientSecret = "secret"),
    )
    private val appModule = AppModule(
        config,
        TestDatabase.module,
        PasswordHasher(cost = 4),
        emailSender = { _, _, _ -> },
        spotifyApi = spotify,
    )

    private fun ApplicationTestBuilder.client(): HttpClient {
        environment { config = MapApplicationConfig() }
        application { bootstrap(appModule) }
        return createClient { install(ContentNegotiation) { json(MusicRoomJson) } }
    }

    /** Signs up and links Spotify through the real connect + callback routes. Returns the access token. */
    private suspend fun HttpClient.connectedUser(): String {
        val auth: AuthResponse = post(ApiRoutes.AUTH_REGISTER) {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("user-${UUID.randomUUID()}@example.com", "hunter42abc", "Alice ${UUID.randomUUID().toString().take(8)}", acceptedTerms = true))
        }.body()
        val connect: SpotifyConnectResponse = post(ApiRoutes.SPOTIFY_CONNECT) { bearerAuth(auth.accessToken) }.body()
        val state = Url(connect.authorizeUrl).parameters["state"]
        get(ApiRoutes.SPOTIFY_CALLBACK) {
            parameter("code", "code")
            parameter("state", state)
        }
        return auth.accessToken
    }

    private suspend fun HttpClient.play(token: String, request: PlayRequest) = put(ApiRoutes.MUSIC_PLAYER_PLAY) {
        bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody(request)
    }

    @Test
    fun `recommendations mix top tracks and top artists, without repeats, and are cached`() = testApplication {
        val client = client()
        val token = client.connectedUser()
        spotify.topTracks = listOf(FakeSpotifyApi.track("t1", "Daft Punk"), FakeSpotifyApi.track("t2", "Justice"))
        spotify.topArtists = listOf(SpotifyArtist(name = "Daft Punk"), SpotifyArtist(name = "Unknown"))
        spotify.searchResults = mapOf(
            "artist:\"Daft Punk\"" to listOf("t1", "d1", "d2", "d3").map { FakeSpotifyApi.track(it, "Daft Punk") },
            // Fewer than 3 new tracks: no section.
            "artist:\"Unknown\"" to listOf(FakeSpotifyApi.track("u1", "Unknown")),
        )

        val first: RecommendationsResponse = client.get(ApiRoutes.MUSIC_RECOMMENDATIONS) { bearerAuth(token) }.body()
        assertEquals(listOf(RecommendationKind.TOP_TRACKS, RecommendationKind.ARTIST), first.sections.map { it.kind })
        assertEquals(listOf("t1", "t2"), first.sections[0].tracks.map { it.id })
        assertEquals("Daft Punk", first.sections[1].artist)
        assertEquals(listOf("d1", "d2", "d3"), first.sections[1].tracks.map { it.id })

        val searches = spotify.searches.size
        val second: RecommendationsResponse = client.get(ApiRoutes.MUSIC_RECOMMENDATIONS) { bearerAuth(token) }.body()
        assertEquals(first, second)
        assertEquals(searches, spotify.searches.size)
    }

    @Test
    fun `play uses the first available device when none is active`() = testApplication {
        val client = client()
        val token = client.connectedUser()
        spotify.devices = listOf(SpotifyDevice(id = null, name = "Restricted"), SpotifyDevice(id = "laptop", name = "Laptop"))

        val response = client.play(token, PlayRequest(listOf("spotify:track:a", "spotify:episode:x", "spotify:track:b"), index = 5))
        assertEquals(HttpStatusCode.NoContent, response.status)
        // Non-track URIs are dropped and the start index stays inside the list.
        assertEquals("laptop" to SpotifyPlayBody(uris = listOf("spotify:track:a", "spotify:track:b"), offset = SpotifyPlayBody.Offset(1)), spotify.plays.last())
    }

    @Test
    fun `a track the device ignores is started again from its album`() = testApplication {
        val client = client()
        val token = client.connectedUser()
        val phone = SpotifyDevice(id = "phone", isActive = true, name = "iPhone")
        spotify.devices = listOf(phone)
        spotify.tracks = mapOf("b" to FakeSpotifyApi.track("b", "Artist").copy(album = SpotifyAlbum(id = "al", uri = "spotify:album:al")))
        // The phone keeps playing what it played before.
        spotify.playbackState = SpotifyPlaybackState(device = phone, isPlaying = true, item = FakeSpotifyApi.track("old", "Other"))

        client.play(token, PlayRequest(listOf("spotify:track:a", "spotify:track:b"), index = 1))
        assertEquals(
            "phone" to SpotifyPlayBody(contextUri = "spotify:album:al", offset = SpotifyPlayBody.Offset(uri = "spotify:track:b")),
            spotify.plays.last(),
        )

        // The track plays (as another version of it): nothing more is sent.
        val linked = FakeSpotifyApi.track("b2", "Artist").copy(linkedFrom = SpotifyTrack.LinkedTrack("spotify:track:b"))
        spotify.playbackState = SpotifyPlaybackState(device = phone, isPlaying = true, item = linked)
        val plays = spotify.plays.size
        client.play(token, PlayRequest(listOf("spotify:track:a", "spotify:track:b"), index = 1))
        assertEquals(plays + 1, spotify.plays.size)
        spotify.playbackState = null
    }

    @Test
    fun `play explains when no device is open or the account is not premium`() = testApplication {
        val client = client()
        val token = client.connectedUser()

        spotify.devices = emptyList()
        val noDevice = client.play(token, PlayRequest())
        assertEquals(HttpStatusCode.Conflict, noDevice.status)
        assertEquals(ApiErrors.SPOTIFY_NO_DEVICE, noDevice.body<ErrorResponse>().error)

        spotify.devices = listOf(SpotifyDevice(id = "phone", isActive = true, name = "Phone"))
        spotify.playError = SpotifyApiException(403, """Spotify 403: {"error":{"reason":"PREMIUM_REQUIRED"}}""")
        val notPremium = client.play(token, PlayRequest())
        assertEquals(HttpStatusCode.Forbidden, notPremium.status)
        assertEquals(ApiErrors.SPOTIFY_PREMIUM_REQUIRED, notPremium.body<ErrorResponse>().error)
        spotify.playError = null
    }

    @Test
    fun `tracks are added to the queue in order on the active device, and the queue lists the next tracks`() = testApplication {
        val client = client()
        val token = client.connectedUser()
        spotify.devices = listOf(SpotifyDevice(id = "laptop", name = "Laptop"), SpotifyDevice(id = "phone", isActive = true, name = "Phone"))
        spotify.queue = listOf(FakeSpotifyApi.track("q1", "Justice"), FakeSpotifyApi.track("q2", "Air"))

        val response = client.post(ApiRoutes.MUSIC_PLAYER_QUEUE) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(QueueRequest(listOf("spotify:track:a", "spotify:episode:x", "spotify:track:b")))
        }
        assertEquals(HttpStatusCode.NoContent, response.status)
        assertEquals(listOf<Pair<String?, String>>("phone" to "spotify:track:a", "phone" to "spotify:track:b"), spotify.queued)

        val queue: QueueResponse = client.get(ApiRoutes.MUSIC_PLAYER_QUEUE) { bearerAuth(token) }.body()
        assertEquals(listOf("q1", "q2"), queue.tracks.map { it.id })
    }

    @Test
    fun `search finds tracks, artists and albums at once`() = testApplication {
        val client = client()
        val token = client.connectedUser()
        spotify.searchResults = mapOf("daft" to listOf(FakeSpotifyApi.track("t1", "Daft Punk")))
        spotify.searchArtists = mapOf("daft" to listOf(SpotifyArtist(id = "a1", name = "Daft Punk", images = listOf(SpotifyImage("big"), SpotifyImage("medium")))))
        spotify.searchAlbums = mapOf("daft" to listOf(SpotifyAlbum(id = "al1", name = "Discovery", albumType = "album", releaseDate = "2001-03-12")))

        val result: SearchResponse = client.get(ApiRoutes.MUSIC_SEARCH_ALL) {
            bearerAuth(token)
            parameter("q", "daft")
        }.body()
        assertEquals(listOf("t1"), result.tracks.map { it.id })
        assertEquals("medium", result.artists.single().imageUrl)
        assertEquals("spotify:artist:a1", result.artists.single().uri)
        assertEquals("2001", result.albums.single().releaseYear)
        assertEquals(AlbumType.ALBUM, result.albums.single().type)
    }

    @Test
    fun `an artist page keeps only their own tracks and lists each album once`() = testApplication {
        val client = client()
        val token = client.connectedUser()
        val daftPunk = SpotifyArtist(id = "a1", name = "Daft Punk", uri = "spotify:artist:a1")
        spotify.artists = mapOf("a1" to daftPunk)
        spotify.searchResults = mapOf(
            "artist:\"Daft Punk\"" to listOf(
                SpotifyTrack(id = "own", name = "One More Time", artists = listOf(daftPunk), uri = "spotify:track:own"),
                SpotifyTrack(id = "cover", name = "Cover", artists = listOf(SpotifyArtist(id = "other", name = "Other")), uri = "spotify:track:cover"),
            ),
        )
        spotify.artistAlbums = mapOf(
            "a1" to listOf(
                SpotifyAlbum(id = "al1", name = "Discovery", albumType = "album"),
                SpotifyAlbum(id = "al2", name = "Discovery", albumType = "album"),
                SpotifyAlbum(id = "s1", name = "Get Lucky", albumType = "single"),
            ),
        )

        val page: ArtistDetailResponse = client.get(ApiRoutes.artist("a1")) { bearerAuth(token) }.body()
        assertEquals("Daft Punk", page.artist.name)
        assertEquals(listOf("own"), page.tracks.map { it.id })
        assertEquals(listOf("al1", "s1"), page.albums.map { it.id })

        // Albums failing on Spotify's side leave the rest of the page.
        spotify.artistAlbums = emptyMap()
        val partial: ArtistDetailResponse = client.get(ApiRoutes.artist("a1")) { bearerAuth(token) }.body()
        assertEquals(listOf("own"), partial.tracks.map { it.id })
        assertTrue(partial.albums.isEmpty())
    }

    @Test
    fun `an album page lists its tracks with its cover and plays as a whole`() = testApplication {
        val client = client()
        val token = client.connectedUser()
        spotify.albums = mapOf(
            "al1" to SpotifyAlbum(
                id = "al1",
                name = "Discovery",
                images = listOf(SpotifyImage("cover")),
                uri = "spotify:album:al1",
                tracks = SpotifyPaging(listOf(SpotifyTrack(id = "t1", name = "One More Time", uri = "spotify:track:t1"))),
            ),
        )

        val page: AlbumDetailResponse = client.get(ApiRoutes.album("al1")) { bearerAuth(token) }.body()
        assertEquals("Discovery", page.album.name)
        assertEquals("cover", page.tracks.single().imageUrl)
        assertEquals("Discovery", page.tracks.single().album)

        spotify.devices = listOf(SpotifyDevice(id = "phone", isActive = true, name = "Phone"))
        client.play(token, PlayRequest(index = 0, contextUri = "spotify:album:al1"))
        assertEquals(SpotifyPlayBody(contextUri = "spotify:album:al1", offset = SpotifyPlayBody.Offset(0)), spotify.plays.last().second)
    }

    @Test
    fun `devices list the usable ones, active first, and playback moves to another one`() = testApplication {
        val client = client()
        val token = client.connectedUser()
        spotify.devices = listOf(
            SpotifyDevice(id = "laptop", name = "Laptop", type = "Computer"),
            SpotifyDevice(id = null, name = "No id"),
            SpotifyDevice(id = "tv", name = "TV", isRestricted = true),
            SpotifyDevice(id = "phone", isActive = true, name = "Phone", type = "Smartphone"),
        )

        val devices: DevicesResponse = client.get(ApiRoutes.MUSIC_PLAYER_DEVICES) { bearerAuth(token) }.body()
        assertEquals(listOf("phone", "laptop"), devices.devices.map { it.id })
        assertEquals(DeviceType.COMPUTER, devices.devices.last().type)

        val moved = client.put(ApiRoutes.MUSIC_PLAYER_DEVICE) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(TransferRequest("laptop", play = false))
        }
        assertEquals(HttpStatusCode.NoContent, moved.status)
        assertEquals("laptop" to false, spotify.transfers.last())
        // Paused before: nothing is started on the new device.
        assertTrue(spotify.plays.isEmpty())

        // Spotify moved the music but left it paused: the server starts it on the new device.
        spotify.playbackState = SpotifyPlaybackState(device = SpotifyDevice(id = "laptop", name = "Laptop"), isPlaying = false)
        client.put(ApiRoutes.MUSIC_PLAYER_DEVICE) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(TransferRequest("laptop"))
        }
        assertEquals("laptop" to null, spotify.plays.single())

        // It plays there already: no extra command.
        spotify.playbackState = SpotifyPlaybackState(device = SpotifyDevice(id = "phone", name = "Phone"), isPlaying = true)
        client.put(ApiRoutes.MUSIC_PLAYER_DEVICE) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(TransferRequest("phone"))
        }
        assertEquals(1, spotify.plays.size)

        val gone = client.put(ApiRoutes.MUSIC_PLAYER_DEVICE) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(TransferRequest("speaker"))
        }
        assertEquals(ApiErrors.SPOTIFY_NO_DEVICE, gone.body<ErrorResponse>().error)
    }

    @Test
    fun `a device playing without saying what takes the track from the currently playing endpoint`() = testApplication {
        val client = client()
        val token = client.connectedUser()
        spotify.playbackState = SpotifyPlaybackState(device = SpotifyDevice(id = "phone", name = "Phone"), isPlaying = true, currentlyPlayingType = "unknown")
        spotify.currentlyPlaying = SpotifyPlaybackState(isPlaying = true, item = FakeSpotifyApi.track("t1", "Daft Punk"))

        val state: PlaybackStateResponse = client.get(ApiRoutes.MUSIC_PLAYER) { bearerAuth(token) }.body()
        assertEquals("t1", state.track?.id)
        assertEquals("Phone", state.deviceName)
    }

    @Test
    fun `player state is inactive when nothing plays`() = testApplication {
        val client = client()
        val token = client.connectedUser()
        spotify.playbackState = null

        val state: PlaybackStateResponse = client.get(ApiRoutes.MUSIC_PLAYER) { bearerAuth(token) }.body()
        assertFalse(state.active)
    }

    @Test
    fun `all playlists are listed across Spotify pages`() = testApplication {
        val client = client()
        val token = client.connectedUser()
        val me = SpotifyPlaylist.Owner(id = "spotify-user")
        spotify.playlists = (1..120).map { SpotifyPlaylist(id = "p$it", name = "Playlist $it", uri = "spotify:playlist:p$it", owner = me) }

        val response: PlaylistsResponse = client.get(ApiRoutes.MUSIC_MY_PLAYLISTS) { bearerAuth(token) }.body()
        assertEquals(120, response.items.size)
        assertEquals("spotify:playlist:p120", response.items.last().uri)
    }

    @Test
    fun `followed playlists are left out, only the user's own are listed`() = testApplication {
        val client = client()
        val token = client.connectedUser()
        spotify.playlists = listOf(
            SpotifyPlaylist(id = "mine", name = "Mine", owner = SpotifyPlaylist.Owner(id = "spotify-user")),
            SpotifyPlaylist(id = "followed", name = "Followed", owner = SpotifyPlaylist.Owner(id = "someone-else")),
        )

        val response: PlaylistsResponse = client.get(ApiRoutes.MUSIC_MY_PLAYLISTS) { bearerAuth(token) }.body()
        assertEquals(listOf("mine"), response.items.map { it.id })
    }

    @Test
    fun `tracks of a followed playlist are refused with a clear code, own playlists list them`() = testApplication {
        val client = client()
        val token = client.connectedUser()
        spotify.playlistItems = mapOf("mine" to listOf(FakeSpotifyApi.track("m1", "Alice")))

        val own: TrackPageResponse = client.get(ApiRoutes.playlistTracks("mine")) { bearerAuth(token) }.body()
        assertEquals(listOf("m1"), own.items.map { it.id })

        val followed = client.get(ApiRoutes.playlistTracks("followed")) { bearerAuth(token) }
        assertEquals(HttpStatusCode.Forbidden, followed.status)
        assertEquals(ApiErrors.SPOTIFY_PLAYLIST_RESTRICTED, followed.body<ErrorResponse>().error)
    }

    @Test
    fun `a playlist plays as a whole from the chosen track`() = testApplication {
        val client = client()
        val token = client.connectedUser()
        spotify.devices = listOf(SpotifyDevice(id = "phone", isActive = true, name = "Phone"))

        client.play(token, PlayRequest(index = 3, contextUri = "spotify:playlist:abc"))
        assertEquals("phone" to SpotifyPlayBody(contextUri = "spotify:playlist:abc", offset = SpotifyPlayBody.Offset(3)), spotify.plays.last())
    }
}
