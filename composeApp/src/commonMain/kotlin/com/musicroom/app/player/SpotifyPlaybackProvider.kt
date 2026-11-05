package com.musicroom.app.player

import com.musicroom.shared.MusicRoomApi

class SpotifyPlaybackProvider(private val api: MusicRoomApi) : PlaybackProvider {
    override suspend fun playbackState() = api.playbackState()
    override suspend fun play(uris: List<String>, index: Int, contextUri: String?) = api.play(uris, index, contextUri)
    override suspend fun pause() = api.pause()
    override suspend fun skipNext() = api.skipNext()
    override suspend fun skipPrevious() = api.skipPrevious()
    override suspend fun seek(positionMs: Long) = api.seek(positionMs)
    override suspend fun devices() = api.devices()
    override suspend fun transferPlayback(deviceId: String, play: Boolean) = api.transferPlayback(deviceId, play)
    override suspend fun queue() = api.queue()
    override suspend fun addToQueue(uris: List<String>) = api.addToQueue(uris)
}
