package com.musicroom.app.music

import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.TrackPageResponse
import com.musicroom.shared.dto.TrackSource

/**
 * Apple Music account connection behind the common [MusicProviderConnection] contract.
 * Wraps the platform [AppleMusicConnection] and supplies the developer token from the API.
 */
class AppleMusicProviderConnection(
    private val api: MusicRoomApi,
    private val connection: AppleMusicConnection,
    private val playback: AppleMusicPlayback,
) : MusicProviderConnection, MusicPlaylistSource {
    override val source = TrackSource.APPLE_MUSIC
    override val available: Boolean get() = connection.available
    override val connected: Boolean get() = connection.connected
    override val displayName: String = "Apple Music"
    val storefront: String? get() = connection.storefront
    override val searchable get() = connected && !storefront.isNullOrBlank()
    override val providesRemotePlayback: Boolean get() = true
    override val providesRecommendations: Boolean get() = true
    override val playlistSource: MusicPlaylistSource get() = this

    override suspend fun playlists(): List<MusicPlaylist> = emptyList()

    override suspend fun tracks(playlist: MusicPlaylist, offset: Int): TrackPageResponse = TrackPageResponse(emptyList(), 0)

    override suspend fun canPlay(): Boolean = available && connected && playback.available && playback.connected

    override suspend fun connect(): Boolean = connection.connect(api.appleMusicDeveloperToken().token)

    override suspend fun disconnect() {
        connection.disconnect()
    }

    override suspend fun search(query: String, mood: String?) = api.searchAppleMusic(mood ?: query, requireNotNull(storefront))

    override suspend fun suggestions() = api.appleMusicCharts(requireNotNull(storefront)).items

    override suspend fun find(isrc: String) = storefront?.let { region ->
        api.matchAppleMusic(isrc, region).items.firstOrNull { it.isrc.equals(isrc, ignoreCase = true) }
    }

    override suspend fun accountLabel(): String? = connection.storefront?.let { "Apple Music ($it)" } ?: "Apple Music"
}
