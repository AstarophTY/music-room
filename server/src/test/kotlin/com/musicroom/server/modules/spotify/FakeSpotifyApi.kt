package com.musicroom.server.modules.spotify

import com.musicroom.server.modules.spotify.client.SpotifyApi
import com.musicroom.server.modules.spotify.client.SpotifyApiException
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

/** In-memory Spotify for tests: records what the server asks and answers with the configured data. */
class FakeSpotifyApi : SpotifyApi {
    var topTracks: List<SpotifyTrack> = emptyList()
    var topArtists: List<SpotifyArtist> = emptyList()
    /** Search results by query. */
    var searchResults: Map<String, List<SpotifyTrack>> = emptyMap()
    var devices: List<SpotifyDevice> = emptyList()
    var playbackState: SpotifyPlaybackState? = null
    var playError: SpotifyApiException? = null
    var playlists: List<SpotifyPlaylist> = emptyList()
    /** Tracks by playlist id; a playlist missing here answers 403 like a followed playlist. */
    var playlistItems: Map<String, List<SpotifyTrack>> = emptyMap()
    /** Tracks found by id; others answer 404. */
    var tracks: Map<String, SpotifyTrack> = emptyMap()

    val searches = mutableListOf<String>()
    val plays = mutableListOf<Pair<String?, SpotifyPlayBody?>>()
    /** Next tracks; [addToQueue] appends to it. */
    var queue: List<SpotifyTrack> = emptyList()
    val queued = mutableListOf<Pair<String?, String>>()

    override fun authorizeUrl(state: String) = "https://accounts.spotify.test/authorize?state=$state"

    override suspend fun exchangeCode(code: String) =
        SpotifyTokenResponse(accessToken = "access", expiresIn = 3600, refreshToken = "refresh")

    override suspend fun refresh(refreshToken: String) = SpotifyTokenResponse(accessToken = "access", expiresIn = 3600)

    override suspend fun me(accessToken: String) = SpotifyUser("spotify-user", "Alice")

    override suspend fun searchTracks(accessToken: String, query: String, offset: Int, limit: Int): SpotifyPaging<SpotifyTrack> {
        searches += query
        return SpotifyPaging(searchResults[query].orEmpty().take(limit))
    }

    override suspend fun track(accessToken: String, id: String) = tracks[id] ?: throw SpotifyApiException(404, "not found")

    override suspend fun myPlaylists(accessToken: String, offset: Int, limit: Int): SpotifyPaging<SpotifyPlaylist> {
        val page = playlists.drop(offset).take(limit)
        val next = if (offset + page.size < playlists.size) "next" else null
        return SpotifyPaging(page, playlists.size, offset, limit, next)
    }

    override suspend fun playlistTracks(accessToken: String, id: String, offset: Int, limit: Int): SpotifyPaging<SpotifyPlaylistTrackItem> {
        val tracks = playlistItems[id] ?: throw SpotifyApiException(403, "Spotify 403: not your playlist")
        return SpotifyPaging(tracks.map { SpotifyPlaylistTrackItem(item = it) })
    }

    override suspend fun topTracks(accessToken: String, limit: Int) = SpotifyPaging(topTracks.take(limit))

    override suspend fun topArtists(accessToken: String, limit: Int, timeRange: String) = SpotifyPaging(topArtists.take(limit))

    override suspend fun playbackState(accessToken: String) = playbackState

    override suspend fun devices(accessToken: String) = devices

    override suspend fun play(accessToken: String, deviceId: String?, body: SpotifyPlayBody?) {
        playError?.let { throw it }
        plays += deviceId to body
    }

    override suspend fun pause(accessToken: String) = Unit

    override suspend fun next(accessToken: String) = Unit

    override suspend fun previous(accessToken: String) = Unit

    override suspend fun seek(accessToken: String, positionMs: Long) = Unit

    override suspend fun queue(accessToken: String) = SpotifyQueue(playbackState?.item, queue)

    override suspend fun addToQueue(accessToken: String, deviceId: String?, uri: String) {
        queued += deviceId to uri
    }

    companion object {
        fun track(id: String, artist: String) =
            SpotifyTrack(id = id, name = "Track $id", artists = listOf(SpotifyArtist(name = artist)), uri = "spotify:track:$id")
    }
}
