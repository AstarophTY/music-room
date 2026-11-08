package com.musicroom.app.music

import com.musicroom.shared.ApiException
import com.musicroom.shared.MusicRoomApi
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
) : MusicProviderConnection {
    override val source = TrackSource.SPOTIFY
    override val available = true
    override var connected: Boolean = false
        private set

    override val displayName: String = "Spotify"

    override val providesRemotePlayback: Boolean get() = true

    override suspend fun connect(): Boolean {
        return try {
            val url = api.spotifyConnect().authorizeUrl
            openUri(url)
            pollUntilConnected()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun disconnect() {
        try {
            api.spotifyDisconnect()
        } catch (_: Exception) {
            // Ignore: the local state is cleared regardless.
        }
        connected = false
    }

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
            if (status != null && status.connected && status.product == "premium") {
                this.connected = true
                return true
            }
        }
        return false
    }
}
