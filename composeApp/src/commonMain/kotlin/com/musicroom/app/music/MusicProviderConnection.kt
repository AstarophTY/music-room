package com.musicroom.app.music

import com.musicroom.app.events.EventTrackResolver

/** Account and catalog capabilities of one music provider. */
interface MusicProviderConnection : MusicCatalogSearch.Provider, EventTrackResolver.Provider {
    val available: Boolean
    val connected: Boolean
    val displayName: String
    val searchable: Boolean
    val providesRemotePlayback: Boolean get() = false
    /** The provider can fill the home screen's recommendation shelves. */
    val providesRecommendations: Boolean get() = false
    suspend fun canPlay(): Boolean
    suspend fun connect(): Boolean
    suspend fun disconnect()

    /** Name of the linked account, when the provider exposes one. */
    suspend fun accountLabel(): String? = null
}
