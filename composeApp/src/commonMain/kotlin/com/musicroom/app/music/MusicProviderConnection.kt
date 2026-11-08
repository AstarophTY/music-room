package com.musicroom.app.music

import com.musicroom.shared.dto.TrackSource

/** A connected music provider account: the same contract for Spotify, Apple Music or any other provider. */
interface MusicProviderConnection {
    /** The catalogue source this provider resolves to. */
    val source: TrackSource
    /** Whether this provider can be used on the current platform at all. */
    val available: Boolean
    /** Whether the user has linked an account for this provider. */
    val connected: Boolean
    /** Human-readable name shown in the account settings. */
    val displayName: String
    /** Whether this provider can control playback remotely (e.g. Spotify Connect). Defaults to false. */
    val providesRemotePlayback: Boolean get() = false
    /** Links the user's account; returns true once linked. */
    suspend fun connect(): Boolean
    /** Unlinks the user's account. */
    suspend fun disconnect()
}
