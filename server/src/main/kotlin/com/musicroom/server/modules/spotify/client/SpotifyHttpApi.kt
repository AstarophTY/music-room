package com.musicroom.server.modules.spotify.client

import com.musicroom.server.config.SpotifyConfig
import com.musicroom.server.modules.spotify.client.model.SpotifyArtist
import com.musicroom.server.modules.spotify.client.model.SpotifyDevice
import com.musicroom.server.modules.spotify.client.model.SpotifyDevicesResponse
import com.musicroom.server.modules.spotify.client.model.SpotifyPaging
import com.musicroom.server.modules.spotify.client.model.SpotifyPlayBody
import com.musicroom.server.modules.spotify.client.model.SpotifyPlaybackState
import com.musicroom.server.modules.spotify.client.model.SpotifyPlaylist
import com.musicroom.server.modules.spotify.client.model.SpotifyPlaylistTrackItem
import com.musicroom.server.modules.spotify.client.model.SpotifyQueue
import com.musicroom.server.modules.spotify.client.model.SpotifySearchResponse
import com.musicroom.server.modules.spotify.client.model.SpotifyTokenResponse
import com.musicroom.server.modules.spotify.client.model.SpotifyTrack
import com.musicroom.server.modules.spotify.client.model.SpotifyUser
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.URLBuilder
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import java.util.Base64

/** Spotify calls over HTTP (Authorization Code flow, the client secret stays on the server). */
class SpotifyHttpApi(private val config: SpotifyConfig) : SpotifyApi {

    @OptIn(ExperimentalSerializationApi::class)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    namingStrategy = JsonNamingStrategy.SnakeCase
                },
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 5_000
        }
    }

    override fun authorizeUrl(state: String): String = URLBuilder(AUTHORIZE_URL).apply {
        parameters.append("client_id", config.clientId)
        parameters.append("response_type", "code")
        parameters.append("redirect_uri", config.redirectUri)
        parameters.append("scope", SCOPES)
        parameters.append("state", state)
    }.buildString()

    override suspend fun exchangeCode(code: String): SpotifyTokenResponse = token(
        parameters {
            append("grant_type", "authorization_code")
            append("code", code)
            append("redirect_uri", config.redirectUri)
        },
    )

    override suspend fun refresh(refreshToken: String): SpotifyTokenResponse = token(
        parameters {
            append("grant_type", "refresh_token")
            append("refresh_token", refreshToken)
        },
    )

    override suspend fun me(accessToken: String): SpotifyUser = api(accessToken, "/me")

    override suspend fun searchTracks(accessToken: String, query: String, offset: Int, limit: Int): SpotifyPaging<SpotifyTrack> =
        api<SpotifySearchResponse>(accessToken, "/search") {
            parameter("q", query)
            parameter("type", "track")
            parameter("offset", offset)
            parameter("limit", limit)
            parameter("market", "from_token")
        }.tracks

    override suspend fun track(accessToken: String, id: String): SpotifyTrack =
        api(accessToken, "/tracks/${id.encodeURLPathPart()}") { parameter("market", "from_token") }

    override suspend fun myPlaylists(accessToken: String, offset: Int, limit: Int): SpotifyPaging<SpotifyPlaylist> =
        api(accessToken, "/me/playlists") {
            parameter("offset", offset)
            parameter("limit", limit)
        }

    override suspend fun playlistTracks(accessToken: String, id: String, offset: Int, limit: Int): SpotifyPaging<SpotifyPlaylistTrackItem> =
        api(accessToken, "/playlists/${id.encodeURLPathPart()}/items") {
            parameter("offset", offset)
            parameter("limit", limit)
            parameter("market", "from_token")
        }

    override suspend fun topTracks(accessToken: String, limit: Int): SpotifyPaging<SpotifyTrack> =
        api(accessToken, "/me/top/tracks") {
            parameter("limit", limit)
            parameter("time_range", "medium_term")
        }

    override suspend fun topArtists(accessToken: String, limit: Int, timeRange: String): SpotifyPaging<SpotifyArtist> =
        api(accessToken, "/me/top/artists") {
            parameter("limit", limit)
            parameter("time_range", timeRange)
        }

    override suspend fun playbackState(accessToken: String): SpotifyPlaybackState? {
        val response = client.get("$API_URL/me/player") {
            bearerAuth(accessToken)
            parameter("market", "from_token")
        }.checked()
        // 204: no device is playing or paused.
        return if (response.status == HttpStatusCode.NoContent) null else response.body()
    }

    override suspend fun devices(accessToken: String): List<SpotifyDevice> =
        api<SpotifyDevicesResponse>(accessToken, "/me/player/devices").devices

    override suspend fun play(accessToken: String, deviceId: String?, body: SpotifyPlayBody?) {
        player(HttpMethod.Put, accessToken, "/me/player/play") {
            if (deviceId != null) parameter("device_id", deviceId)
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }

    override suspend fun pause(accessToken: String) = player(HttpMethod.Put, accessToken, "/me/player/pause")

    override suspend fun next(accessToken: String) = player(HttpMethod.Post, accessToken, "/me/player/next")

    override suspend fun previous(accessToken: String) = player(HttpMethod.Post, accessToken, "/me/player/previous")

    override suspend fun seek(accessToken: String, positionMs: Long) =
        player(HttpMethod.Put, accessToken, "/me/player/seek") { parameter("position_ms", positionMs) }

    override suspend fun queue(accessToken: String): SpotifyQueue = api(accessToken, "/me/player/queue")

    override suspend fun addToQueue(accessToken: String, deviceId: String?, uri: String) =
        player(HttpMethod.Post, accessToken, "/me/player/queue") {
            parameter("uri", uri)
            if (deviceId != null) parameter("device_id", deviceId)
        }

    /** Player commands answer with no useful body. */
    private suspend fun player(
        method: HttpMethod,
        accessToken: String,
        path: String,
        configure: HttpRequestBuilder.() -> Unit = {},
    ) {
        client.request(API_URL + path) {
            this.method = method
            bearerAuth(accessToken)
            configure()
        }.checked()
    }

    private suspend fun token(form: io.ktor.http.Parameters): SpotifyTokenResponse {
        val basic = Base64.getEncoder().encodeToString("${config.clientId}:${config.clientSecret}".toByteArray())
        return client.submitForm(TOKEN_URL, form) { header(HttpHeaders.Authorization, "Basic $basic") }.checked().body()
    }

    private suspend inline fun <reified T> api(
        accessToken: String,
        path: String,
        crossinline configure: HttpRequestBuilder.() -> Unit = {},
    ): T = client.get(API_URL + path) {
        bearerAuth(accessToken)
        configure()
    }.checked().body()

    private suspend fun HttpResponse.checked(): HttpResponse {
        if (status.isSuccess()) return this
        throw SpotifyApiException(status.value, "Spotify ${status.value}: ${bodyAsText().take(300)}")
    }

    private companion object {
        const val AUTHORIZE_URL = "https://accounts.spotify.com/authorize"
        const val TOKEN_URL = "https://accounts.spotify.com/api/token"
        const val API_URL = "https://api.spotify.com/v1"

        /** Profile and playlists for the catalog; playback scopes for the control delegation service. */
        const val SCOPES = "user-read-private playlist-read-private playlist-read-collaborative user-top-read " +
            "user-read-playback-state user-modify-playback-state"
    }
}
