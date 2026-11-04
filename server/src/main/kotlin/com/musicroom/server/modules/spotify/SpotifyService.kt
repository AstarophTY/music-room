package com.musicroom.server.modules.spotify

import com.musicroom.server.common.exceptions.HttpException
import com.musicroom.server.common.jwt.JwtService
import com.musicroom.server.config.SpotifyConfig
import com.musicroom.server.modules.spotify.client.SpotifyApi
import com.musicroom.server.modules.spotify.client.SpotifyApiException
import com.musicroom.server.modules.spotify.entities.SpotifyAccount
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.dto.PlaylistsResponse
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

    suspend fun disconnect(userId: UUID) = accounts.delete(userId)

    suspend fun search(userId: UUID, query: String, offset: Int): TrackPageResponse {
        val page = withToken(userId) { api.searchTracks(it, query.take(200), offset.coerceIn(0, 1000), PAGE_SIZE) }
        return TrackPageResponse(page.items.mapNotNull { it.toResponse() }, page.total, page.nextOffset)
    }

    suspend fun track(userId: UUID, id: String): TrackResponse =
        withToken(userId) { api.track(it, id) }.toResponse()
            ?: throw HttpException(HttpStatusCode.NotFound, "not_found", "Track not found")

    suspend fun myPlaylists(userId: UUID): PlaylistsResponse =
        PlaylistsResponse(withToken(userId) { api.myPlaylists(it, 50) }.items.map { it.toResponse() })

    suspend fun playlistTracks(userId: UUID, playlistId: String, offset: Int): TrackPageResponse {
        val page = withToken(userId) { api.playlistTracks(it, playlistId, offset.coerceAtLeast(0), 100) }
        return TrackPageResponse(page.items.mapNotNull { it.track?.toResponse() }, page.total, page.nextOffset)
    }

    suspend fun topTracks(userId: UUID): TrackPageResponse {
        val page = withToken(userId) { api.topTracks(it, PAGE_SIZE) }
        return TrackPageResponse(page.items.mapNotNull { it.toResponse() }, page.total, null)
    }

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
    }
}
