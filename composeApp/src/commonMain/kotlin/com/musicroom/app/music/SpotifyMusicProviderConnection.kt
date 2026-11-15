package com.musicroom.app.music

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.PlaylistSummaryResponse
import com.musicroom.shared.dto.TrackSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * Spotify account connection behind the common [MusicProviderConnection] contract.
 * The Spotify login opens in the browser and sends the user back to the server, so the app polls the status
 * until the account shows up. [openUri] is invoked by the UI to open the authorization URL.
 */
class SpotifyMusicProviderConnection(
    private val api: MusicRoomApi,
    private val openUri: (String) -> Unit,
    initiallyConnected: Boolean = false,
) : MusicProviderConnection, MusicPlaylistSource {
    override val source = TrackSource.SPOTIFY
    override val available = true
    override var connected by mutableStateOf(initiallyConnected)
        private set

    override val displayName: String = "Spotify"

    override val providesRemotePlayback: Boolean get() = true
    override val providesRecommendations: Boolean get() = true
    override val playlistSource: MusicPlaylistSource get() = this
    override val searchable: Boolean get() = connected
    fun updateConnected(value: Boolean) { connected = value }
    override suspend fun canPlay(): Boolean {
        if (!connected) return false
        val status = api.spotifyStatus()
        return status.connected && status.product == "premium"
    }
    override suspend fun search(query: String, mood: String?) = api.search(query)
    override suspend fun tracks(query: String) = api.searchTracks(query).items
    override suspend fun nextTracks(query: String, offset: Int) = api.searchTracks(query, offset)
    override suspend fun suggestions() = api.myTopTracks().items
    override suspend fun find(isrc: String) = api.searchTracks("isrc:$isrc").items.firstOrNull { it.isrc.equals(isrc, ignoreCase = true) }

    override suspend fun connect(): Boolean {
        val url = api.spotifyConnect().authorizeUrl
        openUri(url)
        return pollUntilConnected()
    }

    override suspend fun disconnect() {
        api.spotifyDisconnect()
        connected = false
    }

    override suspend fun accountLabel(): String? = api.spotifyStatus().displayName

    override suspend fun playlists(): List<MusicPlaylist> = api.myPlaylists().items.map { it.toMusicPlaylist() }

    override suspend fun tracks(playlist: MusicPlaylist, offset: Int) = api.playlistTracks(playlist.id, offset)

    private fun PlaylistSummaryResponse.toMusicPlaylist() = MusicPlaylist(
        source = TrackSource.SPOTIFY,
        id = id,
        name = name,
        imageUrl = imageUrl,
        trackCount = trackCount,
        owner = owner,
        externalUrl = spotifyUrl,
        contextUri = uri,
    )

    private suspend fun pollUntilConnected(): Boolean {
        repeat(90) {
            delay(2_000)
            val status = try {
                api.spotifyStatus()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            if (status != null && status.connected) {
                this.connected = true
                return true
            }
        }
        return false
    }
}
