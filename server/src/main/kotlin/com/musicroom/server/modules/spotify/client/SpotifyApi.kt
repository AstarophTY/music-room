package com.musicroom.server.modules.spotify.client

import com.musicroom.server.modules.spotify.client.model.SpotifyPaging
import com.musicroom.server.modules.spotify.client.model.SpotifyPlaylist
import com.musicroom.server.modules.spotify.client.model.SpotifyPlaylistTrackItem
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

    suspend fun myPlaylists(accessToken: String, limit: Int): SpotifyPaging<SpotifyPlaylist>

    suspend fun playlistTracks(accessToken: String, id: String, offset: Int, limit: Int): SpotifyPaging<SpotifyPlaylistTrackItem>

    suspend fun topTracks(accessToken: String, limit: Int): SpotifyPaging<SpotifyTrack>
}
