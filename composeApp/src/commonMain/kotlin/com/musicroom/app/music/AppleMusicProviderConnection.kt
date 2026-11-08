package com.musicroom.app.music

import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.TrackSource
import kotlinx.coroutines.CancellationException

/**
 * Apple Music account connection behind the common [MusicProviderConnection] contract.
 * Wraps the platform [AppleMusicConnection] and supplies the developer token from the API.
 */
class AppleMusicProviderConnection(
    private val api: MusicRoomApi,
    private val connection: AppleMusicConnection,
) : MusicProviderConnection {
    override val source = TrackSource.APPLE_MUSIC
    override val available: Boolean get() = connection.available
    override val connected: Boolean get() = connection.connected
    override val displayName: String = "Apple Music"
    val storefront: String? get() = connection.storefront

    override suspend fun connect(): Boolean {
        return try {
            connection.connect(api.appleMusicDeveloperToken().token)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun disconnect() {
        connection.disconnect()
    }
}
