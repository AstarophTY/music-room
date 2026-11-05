package com.musicroom.app.player

import com.musicroom.shared.dto.TrackResponse

/** A song added to the queue in the app. [key] tells two entries of the same song apart. */
data class QueueEntry(val key: Long, val track: TrackResponse, val proposer: Proposer?)
