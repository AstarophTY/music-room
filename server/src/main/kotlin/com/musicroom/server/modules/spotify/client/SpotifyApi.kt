package com.musicroom.server.modules.spotify.client

import com.musicroom.server.modules.spotify.client.model.SpotifyArtist
import com.musicroom.server.modules.spotify.client.model.SpotifyDevice
import com.musicroom.server.modules.spotify.client.model.SpotifyPaging
import com.musicroom.server.modules.spotify.client.model.SpotifyPlayBody
import com.musicroom.server.modules.spotify.client.model.SpotifyPlaybackState
import com.musicroom.server.modules.spotify.client.model.SpotifyPlaylist
import com.musicroom.server.modules.spotify.client.model.SpotifyPlaylistTrackItem
import com.musicroom.server.modules.spotify.client.model.SpotifyQueue
import com.musicroom.server.modules.spotify.client.model.SpotifyTokenResponse
import com.musicroom.server.modules.spotify.client.model.SpotifyTrack
import com.musicroom.server.modules.spotify.client.model.SpotifyUser

/** The parts of the Spotify Accounts service and Web API the server uses. Throws [SpotifyApiException]. */
interface SpotifyApi {
    fun authorizeUrl(state: String): String

    suspend fun exchangeCode(code: String): SpotifyTokenResponse

    suspend fun refresh(refreshToken: String): SpotifyTokenResponse

    suspend fun me(accessToken: String): SpotifyUser

    suspend fun searchTracks(accessToken: String, query: String, offset: Int, limit: Int): SpotifyPaging<SpotifyTrack>

    suspend fun track(accessToken: String, id: String): SpotifyTrack

    suspend fun myPlaylists(accessToken: String, offset: Int, limit: Int): SpotifyPaging<SpotifyPlaylist>

    suspend fun playlistTracks(accessToken: String, id: String, offset: Int, limit: Int): SpotifyPaging<SpotifyPlaylistTrackItem>

    suspend fun topTracks(accessToken: String, limit: Int): SpotifyPaging<SpotifyTrack>

    suspend fun topArtists(accessToken: String, limit: Int, timeRange: String): SpotifyPaging<SpotifyArtist>

    /** Null when nothing is playing or paused on any device. */
    suspend fun playbackState(accessToken: String): SpotifyPlaybackState?

    suspend fun devices(accessToken: String): List<SpotifyDevice>

    /** Plays [body] on [deviceId]; a null [body] resumes. */
    suspend fun play(accessToken: String, deviceId: String?, body: SpotifyPlayBody?)

    suspend fun pause(accessToken: String)

    suspend fun next(accessToken: String)

    suspend fun previous(accessToken: String)

    suspend fun seek(accessToken: String, positionMs: Long)

    suspend fun queue(accessToken: String): SpotifyQueue

    /** Adds the track [uri] at the end of the queue of [deviceId]. */
    suspend fun addToQueue(accessToken: String, deviceId: String?, uri: String)
}
