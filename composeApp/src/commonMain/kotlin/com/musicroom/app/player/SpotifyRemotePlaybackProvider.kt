package com.musicroom.app.player

import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.error_spotify_not_connected
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.ApiException
import com.musicroom.shared.dto.TrackSource

interface SpotifyRemotePlaybackProvider : RemotePlaybackProvider {
    override val source: TrackSource get() = TrackSource.SPOTIFY
    override fun notConnectedError(): UiText = UiText.of(Res.string.error_spotify_not_connected)
    override fun isDisconnected(error: Exception) = (error as? ApiException)?.error == ApiErrors.SPOTIFY_NOT_CONNECTED
    override fun isMissingDevice(error: Exception) = (error as? ApiException)?.error == ApiErrors.SPOTIFY_NO_DEVICE
}
