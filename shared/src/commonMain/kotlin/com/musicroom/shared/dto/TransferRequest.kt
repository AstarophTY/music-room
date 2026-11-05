package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Moves playback to [deviceId]; [play] starts it there, otherwise it keeps its current state. */
@Serializable
data class TransferRequest(val deviceId: String, val play: Boolean = true)
