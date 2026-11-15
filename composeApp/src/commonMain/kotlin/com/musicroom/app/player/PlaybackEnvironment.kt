package com.musicroom.app.player

/** Wires remote and device playback adapters at the application boundary. */
class PlaybackEnvironment(
    val remote: RemotePlaybackProvider,
    private val createDevices: (onEnded: () -> Unit, onError: (Exception) -> Unit) -> DevicePlaybackProviders,
) {
    fun devices(onEnded: () -> Unit, onError: (Exception) -> Unit) = createDevices(onEnded, onError)
}
