package com.musicroom.app.music

import com.musicroom.app.events.EventTrackResolver
import kotlinx.coroutines.CancellationException

/** Selects the capabilities of connected provider adapters. */
class MusicProviderRegistry(private val connections: List<MusicProviderConnection>) {
    fun catalogSearch() = MusicCatalogSearch(connections.filter { it.available && it.connected && it.searchable })

    suspend fun playbackAvailability(): MusicPlaybackAvailability {
        val playable = connections.filter { provider ->
            if (!provider.available || !provider.connected) false else try {
                provider.canPlay()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
        }
        return MusicPlaybackAvailability(
            EventTrackResolver(playable),
            playable.map { it.source }.toSet(),
            playable.filter { it.providesRemotePlayback }.map { it.source }.toSet(),
        )
    }
}
