package com.musicroom.server.modules.spotify

import com.musicroom.server.common.exceptions.BadRequestException
import com.musicroom.server.common.exceptions.HttpException
import com.musicroom.server.common.jwt.JwtService
import com.musicroom.server.config.SpotifyConfig
import com.musicroom.server.modules.spotify.client.SpotifyApi
import com.musicroom.server.modules.spotify.client.SpotifyApiException
import com.musicroom.server.modules.spotify.entities.SpotifyAccount
import com.musicroom.shared.ApiErrors
import com.musicroom.server.modules.spotify.client.model.SpotifyPlayBody
import com.musicroom.shared.dto.AlbumDetailResponse
import com.musicroom.shared.dto.ArtistDetailResponse
import com.musicroom.shared.dto.DevicesResponse
import com.musicroom.shared.dto.PlayRequest
import com.musicroom.shared.dto.PlaybackStateResponse
import com.musicroom.shared.dto.PlaylistSummaryResponse
import com.musicroom.shared.dto.PlaylistsResponse
import com.musicroom.shared.dto.QueueResponse
import com.musicroom.shared.dto.RecommendationKind
import com.musicroom.shared.dto.RecommendationSection
import com.musicroom.shared.dto.RecommendationsResponse
import com.musicroom.shared.dto.SearchResponse
import com.musicroom.shared.dto.SpotifyConnectResponse
import com.musicroom.shared.dto.SpotifyStatusResponse
import com.musicroom.shared.dto.TrackPageResponse
import com.musicroom.shared.dto.TrackResponse
import com.musicroom.shared.dto.TransferRequest
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Connects users' Spotify accounts and reads the catalog with their token, or with the app's own token for users
 * who did not link Spotify (they can still search, e.g. to propose songs in an event). Spotify is only used for the
 * catalog and playback; votes, rights and playlists stay in Music Room.
 */
class SpotifyService(
    private val config: SpotifyConfig,
    private val api: SpotifyApi,
    private val accounts: SpotifyAccountsRepository,
    private val jwt: JwtService,
) {
    private val log = LoggerFactory.getLogger(SpotifyService::class.java)
    private val refreshLock = Mutex()
    private val appTokenLock = Mutex()
    @Volatile
    private var appToken: Pair<String, OffsetDateTime>? = null
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
        val page = withCatalogToken(userId) { api.searchTracks(it, query.take(200), offset.coerceIn(0, 1000), SEARCH_PAGE_SIZE) }
        return TrackPageResponse(page.items.mapNotNull { it.toResponse() }, page.total, page.nextOffset)
    }

    /** Tracks, artists and albums matching [query], in one Spotify request. */
    suspend fun searchAll(userId: UUID, query: String): SearchResponse {
        val result = withCatalogToken(userId) { api.search(it, query.take(200), listOf("track", "artist", "album"), SEARCH_PAGE_SIZE) }
        return SearchResponse(
            tracks = result.tracks.items.mapNotNull { it.toResponse() },
            artists = result.artists.items.mapNotNull { it.toResponse() },
            albums = result.albums.items.mapNotNull { it.toResponse() },
            nextTrackOffset = result.tracks.nextOffset,
        )
    }

    /**
     * An artist with their popular tracks (found by a search on their name, keeping only their own tracks, as
     * for the recommendations) and their albums and singles. A part that fails is left empty.
     */
    suspend fun artist(userId: UUID, id: String): ArtistDetailResponse {
        val artist = withCatalogToken(userId) { api.artist(it, id) }
        val response = artist.toResponse() ?: throw HttpException(HttpStatusCode.NotFound, "not_found", "Artist not found")
        val tracks = optional("artist tracks") {
            withCatalogToken(userId) { api.searchTracks(it, "artist:\"${artist.name}\"", 0, SEARCH_PAGE_SIZE) }
                .items.filter { track -> track.artists.any { it.id == id } }.mapNotNull { it.toResponse() }
        }
        val albums = optional("artist albums") {
            withCatalogToken(userId) { api.artistAlbums(it, id, ARTIST_ALBUMS) }.items.mapNotNull { it.toResponse() }
                // Spotify lists the same album once per market or edition.
                .distinctBy { it.name.lowercase() to it.type }
        }
        return ArtistDetailResponse(response, tracks, albums)
    }

    suspend fun album(userId: UUID, id: String): AlbumDetailResponse {
        val album = withCatalogToken(userId) { api.album(it, id) }
        val response = album.toResponse() ?: throw HttpException(HttpStatusCode.NotFound, "not_found", "Album not found")
        return AlbumDetailResponse(response, album.tracks?.items.orEmpty().mapNotNull { it.toResponse(fromAlbum = album) })
    }

    /** Runs [load], or gives an empty list when Spotify fails (but not when the account is not linked). */
    private suspend fun <T> optional(what: String, load: suspend () -> List<T>): List<T> = try {
        load()
    } catch (e: HttpException) {
        if (e.error == ApiErrors.SPOTIFY_NOT_CONNECTED || e.error == ApiErrors.SPOTIFY_NOT_CONFIGURED) throw e
        log.warn("Spotify {} unavailable: {}", what, e.message)
        emptyList()
    }

    suspend fun track(userId: UUID, id: String): TrackResponse =
        withCatalogToken(userId) { api.track(it, id) }.toResponse()
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

    /**
     * What plays on the user's devices. Right after the music moves to another device, Spotify can report it
     * playing without saying what: the "currently playing" endpoint is asked then.
     */
    suspend fun playbackState(userId: UUID): PlaybackStateResponse {
        val state = withToken(userId) { api.playbackState(it) } ?: return PlaybackStateResponse(active = false)
        var item = state.item
        if (item == null && state.device != null) {
            item = try {
                withToken(userId) { api.currentlyPlaying(it) }?.item
            } catch (e: HttpException) {
                null
            }
            if (item == null) {
                log.info(
                    "Spotify reports playback without a track: type={}, playing={}, device type={}",
                    state.currentlyPlayingType, state.isPlaying, state.device.type,
                )
            }
        }
        return PlaybackStateResponse(
            active = true,
            isPlaying = state.isPlaying,
            track = item?.toResponse(),
            progressMs = state.progressMs ?: 0,
            deviceName = state.device?.name,
        )
    }

    /**
     * Plays on the active device, or on the first available one (the Spotify app is open but idle).
     * A playlist or album context wins over track URIs; only track URIs are accepted, up to 100.
     */
    suspend fun play(userId: UUID, request: PlayRequest) {
        val uris = request.uris.filter { it.startsWith("spotify:track:") }.take(MAX_PLAY_URIS)
        val context = request.contextUri?.takeIf { it.startsWith("spotify:playlist:") || it.startsWith("spotify:album:") }
        val startIndex = request.index.coerceIn(0, uris.lastIndex.coerceAtLeast(0))
        val start = uris.getOrNull(startIndex)
        val body = when {
            context != null -> SpotifyPlayBody(contextUri = context, offset = SpotifyPlayBody.Offset(request.index.coerceAtLeast(0)))
            start != null -> SpotifyPlayBody(uris = uris, offset = SpotifyPlayBody.Offset(startIndex))
            else -> null
        }
        withToken(userId) { token ->
            playerCall {
                val device = deviceId(token)
                api.play(token, device, body)
                if (context == null && start != null) ensureTrackStarts(token, device, start)
            }
        }
    }

    /**
     * The Spotify app on iPhone can accept a list of tracks and then ignore it (it plays nothing, or resumes
     * what it played before), while it starts albums and playlists right away. When [uri] does not play after a
     * moment, it is started again from its album.
     */
    private suspend fun ensureTrackStarts(token: String, deviceId: String?, uri: String) {
        repeat(TRACK_START_CHECKS) {
            delay(TRACK_START_CHECK_INTERVAL_MS)
            val item = api.playbackState(token)?.item
            if (item?.uri == uri || item?.linkedFrom?.uri == uri) return
        }
        val album = try {
            api.track(token, uri.removePrefix("spotify:track:")).album?.uri
        } catch (e: SpotifyApiException) {
            null
        }
        if (album.isNullOrBlank()) {
            log.info("Track did not start on the device and its album is unknown")
            return
        }
        log.info("Track did not start on the device, starting it from its album")
        api.play(token, deviceId, SpotifyPlayBody(contextUri = album, offset = SpotifyPlayBody.Offset(uri = uri)))
    }

    /** The devices the app can play on, the active one first. */
    suspend fun devices(userId: UUID): DevicesResponse =
        DevicesResponse(withToken(userId) { api.devices(it) }.mapNotNull { it.toResponse() }.sortedByDescending { it.isActive })

    /** Moves playback to [request]'s device; Spotify answers 404 when that device is gone. */
    suspend fun transfer(userId: UUID, request: TransferRequest) {
        if (request.deviceId.isBlank()) throw BadRequestException("deviceId is required")
        withToken(userId) { token ->
            playerCall {
                api.transfer(token, request.deviceId, request.play)
                if (request.play) ensurePlayingOn(token, request.deviceId)
            }
        }
    }

    /**
     * Spotify sometimes moves the music to a device that was idle (a phone in the background) but leaves it
     * paused there: checks for a moment that it plays on [deviceId], otherwise starts it there.
     */
    private suspend fun ensurePlayingOn(token: String, deviceId: String) {
        repeat(TRANSFER_CHECKS) {
            delay(TRANSFER_CHECK_INTERVAL_MS)
            val state = api.playbackState(token)
            if (state?.device?.id == deviceId && state.isPlaying) return
        }
        log.info("Playback did not start after moving it to another device, starting it there")
        api.play(token, deviceId, null)
    }

    /** Empty when nothing plays (Spotify then has no queue to show). */
    suspend fun queue(userId: UUID): QueueResponse {
        val queue = try {
            withToken(userId) { api.queue(it) }
        } catch (e: HttpException) {
            if (e.error == ApiErrors.SPOTIFY_NOT_CONNECTED || e.error == ApiErrors.SPOTIFY_NOT_CONFIGURED) throw e
            log.info("Spotify queue unavailable: {}", e.message)
            return QueueResponse()
        }
        return QueueResponse(queue.queue.mapNotNull { it.toResponse() })
    }

    /** Adds track URIs at the end of the queue, one Spotify call each (Spotify takes one at a time), up to 50. */
    suspend fun addToQueue(userId: UUID, uris: List<String>) {
        val tracks = uris.filter { it.startsWith("spotify:track:") }.take(MAX_QUEUE_URIS)
        if (tracks.isEmpty()) throw BadRequestException("No track to add to the queue")
        withToken(userId) { token ->
            playerCall {
                val device = deviceId(token)
                tracks.forEach { api.addToQueue(token, device, it) }
            }
        }
    }

    /** The active device, or the first available one (the Spotify app is open but idle). */
    private suspend fun deviceId(token: String): String? {
        val devices = api.devices(token).filter { it.id != null && !it.isRestricted }
        return (devices.firstOrNull { it.isActive } ?: devices.firstOrNull() ?: throw noDevice()).id
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

    /** Like [withToken] for catalog reads: with the user's token when they linked Spotify, the app's token otherwise. */
    private suspend fun <T> withCatalogToken(userId: UUID, call: suspend (String) -> T): T {
        if (accounts.find(userId) != null) return withToken(userId, call)
        if (!config.configured) {
            throw HttpException(HttpStatusCode.ServiceUnavailable, ApiErrors.SPOTIFY_NOT_CONFIGURED, "Spotify is not configured on the server")
        }
        return try {
            spotifyCall { call(appToken(force = false)) }
        } catch (e: SpotifyApiException) {
            if (e.status != 401) throw e
            spotifyCall { call(appToken(force = true)) }
        }
    }

    /** The app's token, asked again a minute before it expires (or when [force]d after a 401). */
    private suspend fun appToken(force: Boolean): String = appTokenLock.withLock {
        val cached = appToken
        if (!force && cached != null && cached.second.isAfter(OffsetDateTime.now().plusSeconds(60))) return cached.first
        val tokens = try {
            api.appToken()
        } catch (e: SpotifyApiException) {
            throw unavailable(e)
        }
        appToken = tokens.accessToken to expiry(tokens.expiresIn)
        tokens.accessToken
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
        const val MAX_QUEUE_URIS = 50
        const val ARTIST_ALBUMS = 10

        /** After moving playback: up to 4 checks, half a second apart, that the music plays on the new device. */
        const val TRANSFER_CHECKS = 4
        const val TRANSFER_CHECK_INTERVAL_MS = 500L

        /** After playing a list of tracks: up to 5 checks, half a second apart, that the chosen one plays. */
        const val TRACK_START_CHECKS = 5
        const val TRACK_START_CHECK_INTERVAL_MS = 500L
        const val PLAYLISTS_PAGE_SIZE = 50
        const val MAX_PLAYLISTS = 500
        const val RECOMMENDATIONS_TTL_MS = 10 * 60 * 1000L
    }
}
