package com.musicroom.app.player

import com.musicroom.shared.dto.TrackSource

/** Selects device-local playback implementations by track source. */
class DevicePlaybackProviders(private val sessions: List<DevicePlaybackSession>) {
    fun find(source: TrackSource): DevicePlaybackSession? = sessions.firstOrNull { it.source == source }
    val all: List<DevicePlaybackSession> get() = sessions
}
