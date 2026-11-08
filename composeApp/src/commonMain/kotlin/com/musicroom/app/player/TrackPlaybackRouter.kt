package com.musicroom.app.player

import com.musicroom.app.common.UiText
import com.musicroom.shared.dto.TrackResponse
import com.musicroom.shared.dto.TrackSource

/** Routes tracks to their playback implementation and describes who manages their queue. */
class TrackPlaybackRouter(private val providers: List<Provider>) {
    interface Provider {
        val source: TrackSource
        val managesOwnQueue: Boolean
        fun availabilityError(): UiText? = null
        fun connectionError(): UiText? = null
        fun play(track: TrackResponse)
    }

    fun availabilityError(track: TrackResponse): UiText? = provider(track).availabilityError()
    fun playbackError(track: TrackResponse): UiText? = provider(track).let { it.availabilityError() ?: it.connectionError() }
    fun appManaged(track: TrackResponse): Boolean = provider(track).managesOwnQueue.not()
    fun play(track: TrackResponse) = provider(track).play(track)

    private fun provider(track: TrackResponse): Provider =
        providers.firstOrNull { it.source == track.source } ?: error("No playback provider for ${track.source}")
}
