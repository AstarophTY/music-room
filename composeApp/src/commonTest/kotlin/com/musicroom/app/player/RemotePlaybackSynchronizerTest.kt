package com.musicroom.app.player

import com.musicroom.shared.dto.PlaybackStateResponse
import com.musicroom.shared.dto.TrackResponse
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemotePlaybackSynchronizerTest {
    @Test
    fun ignoresOldStateUntilExpectedTrackAndDeviceStartPlaying() {
        val sync = RemotePlaybackSynchronizer()
        val track = TrackResponse("track", "Track", emptyList(), "", durationMs = 1000, uri = "track")
        sync.waitFor(track.id, device = "Speaker", playing = true)
        assertTrue(sync.waiting)
        assertTrue(sync.isStillStarting(PlaybackStateResponse(active = true, isPlaying = true, track = track, deviceName = "Phone")))
        assertTrue(sync.isStillStarting(PlaybackStateResponse(active = true, isPlaying = false, track = track, deviceName = "Speaker")))
        assertFalse(sync.isStillStarting(PlaybackStateResponse(active = true, isPlaying = true, track = track, deviceName = "Speaker")))
        assertFalse(sync.waiting)
    }
}
