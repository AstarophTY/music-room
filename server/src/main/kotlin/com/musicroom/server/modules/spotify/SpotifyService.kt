package com.musicroom.server.modules.spotify

import com.musicroom.server.common.exceptions.HttpException
import com.musicroom.server.common.jwt.JwtService
import com.musicroom.server.config.SpotifyConfig
import com.musicroom.server.modules.spotify.client.SpotifyApi
import com.musicroom.server.modules.spotify.client.SpotifyApiException
import com.musicroom.server.modules.spotify.entities.SpotifyAccount
import com.musicroom.shared.ApiErrors
import com.musicroom.server.modules.spotify.client.model.SpotifyPlayBody
import com.musicroom.shared.dto.PlayRequest
import com.musicroom.shared.dto.PlaybackStateResponse
import com.musicroom.shared.dto.PlaylistSummaryResponse
import com.musicroom.shared.dto.PlaylistsResponse
import com.musicroom.shared.dto.RecommendationKind
import com.musicroom.shared.dto.RecommendationSection
import com.musicroom.shared.dto.RecommendationsResponse
import com.musicroom.shared.dto.SpotifyConnectResponse
import com.musicroom.shared.dto.SpotifyStatusResponse
import com.musicroom.shared.dto.TrackPageResponse
import com.musicroom.shared.dto.TrackResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Connects users' Spotify accounts and reads the catalog with their token. Spotify is only used for the catalog
 * and playback; votes, rights and playlists stay in Music Room.
 */
class SpotifyService(
    private val config: SpotifyConfig,
    private val api: SpotifyApi,
    private val accounts: SpotifyAccountsRepository,
    private val jwt: JwtService,
) {
    private val log = LoggerFactory.getLogger(SpotifyService::class.java)
    private val refreshLock = Mutex()
    private val recommendationsCache = ConcurrentHashMap<UUID, Pair<Long, RecommendationsResponse>>()

    suspend fun status(userId: UUID): SpotifyStatusResponse {
        val account = accounts.find(userId)
        return SpotifyStatusResponse(config.configured, account != null, account?.displayName, account?.product)
    }

    /** Spotify login page. The state is a signed, short-lived token: it says which user the callback is for. */
    fun connect(userId: UUID): SpotifyConnectResponse {
        if (!config.configured) {
            throw HttpException(HttpStatusCode.ServiceUnavailable, ApiErrors.SPOTIFY_NOT_CONFIGURED, "Spotify is not configured on the server")
        }
        return SpotifyConnectResponse(api.authorizeUrl(jwt.createSpotifyState(userId)))
    }

    suspend fun handleCallback(code: String?, state: String?, error: String?): SpotifyCallbackOutcome {
        if (error != null) return SpotifyCallbackOutcome.Denied
        val userId = state?.let(jwt::verifySpotifyState) ?: return SpotifyCallbackOutcome.Expired
        if (code == null) return SpotifyCallbackOutcome.Failed
        return try {
            val tokens = api.exchangeCode(code)
            val me = api.me(tokens.accessToken)
            accounts.save(
                SpotifyAccount(
                    userId = userId,
                    spotifyUserId = me.id,
                    displayName = me.displayName,
                    product = me.product,
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken ?: return SpotifyCallbackOutcome.Failed,
                    expiresAt = expiry(tokens.expiresIn),
                ),
                tokens.scope,
            )
            SpotifyCallbackOutcome.Connected
        } catch (e: SpotifyApiException) {
            log.warn("Spotify connection failed for user {}: {}", userId, e.message)
            if (e.status == 403) SpotifyCallbackOutcome.NotAllowed else SpotifyCallbackOutcome.Failed
        }
    }

    suspend fun disconnect(userId: UUID) {
        recommendationsCache.remove(userId)
        accounts.delete(userId)
    }

    suspend fun search(userId: UUID, query: String, offset: Int): TrackPageResponse {
        val page = withToken(userId) { api.searchTracks(it, query.take(200), offset.coerceIn(0, 1000), SEARCH_PAGE_SIZE) }
        return TrackPageResponse(page.items.mapNotNull { it.toResponse() }, page.total, page.nextOffset)
    }

    suspend fun track(userId: UUID, id: String): TrackResponse =
        withToken(userId) { api.track(it, id) }.toResponse()
            ?: throw HttpException(HttpStatusCode.NotFound, "not_found", "Track not found")

    /**
     * The playlists the user created on Spotify (followed ones are left out), 50 per Spotify request,
     * up to [MAX_PLAYLISTS].
     */
    suspend fun myPlaylists(userId: UUID): PlaylistsResponse {
        val owner = accounts.find(userId)?.spotifyUserId ?: throw notConnected()
        val playlists = mutableListOf<PlaylistSummaryResponse>()
        var offset: Int? = 0
        while (offset != null && playlists.size < MAX_PLAYLISTS) {
            val page = withToken(userId) { api.myPlaylists(it, offset ?: 0, PLAYLISTS_PAGE_SIZE) }
            playlists += page.items.filter { it.owner?.id == owner }.map { it.toResponse() }
            offset = page.nextOffset
        }
        return PlaylistsResponse(playlists.distinctBy { it.id })
    }

    /** Spotify only lists the tracks of the user's own playlists; others answer [ApiErrors.SPOTIFY_PLAYLIST_RESTRICTED]. */
    suspend fun playlistTracks(userId: UUID, playlistId: String, offset: Int): TrackPageResponse {
        val page = withToken(userId) { token ->
            try {
                api.playlistTracks(token, playlistId, offset.coerceAtLeast(0), PLAYLISTS_PAGE_SIZE)
            } catch (e: SpotifyApiException) {
                if (e.status != 403) throw e
                throw HttpException(
                    HttpStatusCode.Forbidden,
                    ApiErrors.SPOTIFY_PLAYLIST_RESTRICTED,
                    "Spotify only shows the tracks of your own playlists",
                )
            }
        }
        return TrackPageResponse(page.items.mapNotNull { it.content?.toResponse() }, page.total, page.nextOffset)
    }

    suspend fun topTracks(userId: UUID): TrackPageResponse {
        val page = withToken(userId) { api.topTracks(it, PAGE_SIZE) }
        return TrackPageResponse(page.items.mapNotNull { it.toResponse() }, page.total, null)
    }

    /**
     * Spotify closed its recommendations endpoint to new apps, so they are built here: the user's top tracks,
     * then more tracks from their top artists (found with search). Cached a few minutes to save Spotify quota.
     */
    suspend fun recommendations(userId: UUID): RecommendationsResponse {
        recommendationsCache[userId]?.let { (time, cached) ->
            if (System.currentTimeMillis() - time < RECOMMENDATIONS_TTL_MS) return cached
        }
        val topTracks = withToken(userId) { api.topTracks(it, TOP_TRACKS) }.items.mapNotNull { it.toResponse() }
        val artists = withToken(userId) { api.topArtists(it, TOP_ARTISTS, "short_term") }.items
            .ifEmpty { withToken(userId) { api.topArtists(it, TOP_ARTISTS, "medium_term") }.items }
        val seen = topTracks.mapTo(mutableSetOf()) { it.id }
        val sections = mutableListOf<RecommendationSection>()
        if (topTracks.isNotEmpty()) sections += RecommendationSection(RecommendationKind.TOP_TRACKS, topTracks)
        for (artist in artists.distinctBy { it.name }.take(ARTIST_SECTIONS)) {
            val tracks = try {
                withToken(userId) { api.searchTracks(it, "artist:\"${artist.name}\"", 0, SEARCH_PAGE_SIZE) }
                    .items.mapNotNull { it.toResponse() }
            } catch (e: HttpException) {
                // One artist failing (rate limit, Spotify hiccup) should not hide the other sections.
                if (e.error == ApiErrors.SPOTIFY_NOT_CONNECTED) throw e
                log.warn("Recommendations search failed for {}: {}", artist.name, e.message)
                continue
            }.filter { seen.add(it.id) }
            if (tracks.size >= MIN_SECTION_TRACKS) sections += RecommendationSection(RecommendationKind.ARTIST, tracks, artist.name)
        }
        return RecommendationsResponse(sections).also { recommendationsCache[userId] = System.currentTimeMillis() to it }
    }

    suspend fun playbackState(userId: UUID): PlaybackStateResponse {
        val state = withToken(userId) { api.playbackState(it) } ?: return PlaybackStateResponse(active = false)
        return PlaybackStateResponse(
            active = true,
            isPlaying = state.isPlaying,
            track = state.item?.toResponse(),
            progressMs = state.progressMs ?: 0,
            deviceName = state.device?.name,
        )
    }

    /**
     * Plays on the active device, or on the first available one (the Spotify app is open but idle).
     * A playlist context wins over track URIs; only track URIs are accepted, up to 100.
     */
    suspend fun play(userId: UUID, request: PlayRequest) {
        val uris = request.uris.filter { it.startsWith("spotify:track:") }.take(MAX_PLAY_URIS)
        val context = request.contextUri?.takeIf { it.startsWith("spotify:playlist:") }
        val body = when {
            context != null -> SpotifyPlayBody(contextUri = context, offset = SpotifyPlayBody.Offset(request.index.coerceAtLeast(0)))
            uris.isNotEmpty() -> SpotifyPlayBody(uris = uris, offset = SpotifyPlayBody.Offset(request.index.coerceIn(0, uris.lastIndex)))
            else -> null
        }
        withToken(userId) { token ->
            playerCall {
                val devices = api.devices(token).filter { it.id != null && !it.isRestricted }
                val device = devices.firstOrNull { it.isActive } ?: devices.firstOrNull() ?: throw noDevice()
                api.play(token, device.id, body)
            }
        }
    }

    suspend fun pause(userId: UUID) = withToken(userId) { token -> playerCall { api.pause(token) } }

    suspend fun next(userId: UUID) = withToken(userId) { token -> playerCall { api.next(token) } }

    suspend fun previous(userId: UUID) = withToken(userId) { token -> playerCall { api.previous(token) } }

    suspend fun seek(userId: UUID, positionMs: Long) =
        withToken(userId) { token -> playerCall { api.seek(token, positionMs.coerceAtLeast(0)) } }

    /**
     * Player errors: 404 means no device is available, 403 that the account is not Premium. Another 403
     * ("restriction violated", e.g. pausing what is already paused) changes nothing, so it is ignored.
     */
    private suspend fun playerCall(call: suspend () -> Unit) {
        try {
            call()
        } catch (e: SpotifyApiException) {
            when {
                e.status == 404 -> throw noDevice()
                e.status == 403 && e.message.orEmpty().contains("PREMIUM_REQUIRED") -> throw premiumRequired()
                e.status == 403 -> log.info("Spotify refused a player command: {}", e.message)
                else -> throw e
            }
        }
    }

    private fun noDevice() =
        HttpException(HttpStatusCode.Conflict, ApiErrors.SPOTIFY_NO_DEVICE, "Open Spotify on one of your devices first")

    private fun premiumRequired() =
        HttpException(HttpStatusCode.Forbidden, ApiErrors.SPOTIFY_PREMIUM_REQUIRED, "Playback needs Spotify Premium")

    /**
     * Runs [call] with a valid access token: refreshes it when it is about to expire, and once more if Spotify
     * still answers 401. A refused refresh means the user revoked access: the account is disconnected.
     */
    private suspend fun <T> withToken(userId: UUID, call: suspend (String) -> T): T {
        if (!config.configured) {
            throw HttpException(HttpStatusCode.ServiceUnavailable, ApiErrors.SPOTIFY_NOT_CONFIGURED, "Spotify is not configured on the server")
        }
        var account = accounts.find(userId) ?: throw notConnected()
        if (account.expiresAt.isBefore(OffsetDateTime.now().plusSeconds(60))) account = refresh(account)
        return try {
            spotifyCall { call(account.accessToken) }
        } catch (e: SpotifyApiException) {
            if (e.status != 401) throw e
            spotifyCall { call(refresh(account).accessToken) }
        }
    }

    private suspend fun refresh(account: SpotifyAccount): SpotifyAccount = refreshLock.withLock {
        // Another request may have refreshed it while we waited.
        val current = accounts.find(account.userId) ?: throw notConnected()
        if (current.accessToken != account.accessToken) return current
        val tokens = try {
            api.refresh(current.refreshToken)
        } catch (e: SpotifyApiException) {
            if (e.status == 400 || e.status == 401) {
                accounts.delete(account.userId)
                throw notConnected()
            }
            throw unavailable(e)
        }
        val updated = current.copy(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken ?: current.refreshToken,
            expiresAt = expiry(tokens.expiresIn),
        )
        accounts.updateTokens(updated.userId, updated.accessToken, updated.refreshToken, updated.expiresAt)
        updated
    }

    /** Turns Spotify errors (other than 401) into API errors the app understands. */
    private suspend fun <T> spotifyCall(call: suspend () -> T): T = try {
        call()
    } catch (e: CancellationException) {
        throw e
    } catch (e: SpotifyApiException) {
        when (e.status) {
            401 -> throw e
            404 -> throw HttpException(HttpStatusCode.NotFound, "not_found", "Not found on Spotify")
            429 -> throw HttpException(HttpStatusCode.TooManyRequests, ApiErrors.SPOTIFY_RATE_LIMITED, "Spotify is busy, try again in a moment")
            else -> throw unavailable(e)
        }
    }

    private fun unavailable(e: Exception): HttpException {
        log.warn("Spotify error: {}", e.message)
        return HttpException(HttpStatusCode.BadGateway, ApiErrors.SPOTIFY_UNAVAILABLE, "Spotify did not answer, try again later")
    }

    private fun notConnected() =
        HttpException(HttpStatusCode.Conflict, ApiErrors.SPOTIFY_NOT_CONNECTED, "Connect your Spotify account first")

    private fun expiry(expiresInSeconds: Long) = OffsetDateTime.now().plusSeconds(expiresInSeconds)

    private companion object {
        const val PAGE_SIZE = 20

        /** Spotify search returns at most 10 results per request for apps in development mode. */
        const val SEARCH_PAGE_SIZE = 10
        const val TOP_TRACKS = 10
        const val TOP_ARTISTS = 5
        const val ARTIST_SECTIONS = 3
        const val MIN_SECTION_TRACKS = 3
        const val MAX_PLAY_URIS = 100
        const val PLAYLISTS_PAGE_SIZE = 50
        const val MAX_PLAYLISTS = 500
        const val RECOMMENDATIONS_TTL_MS = 10 * 60 * 1000L
    }
}
