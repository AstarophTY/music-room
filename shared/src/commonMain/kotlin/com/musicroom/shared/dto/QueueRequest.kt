package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Adds [uris] (Spotify track URIs) at the end of the queue, in this order. */
@Serializable
data class QueueRequest(val uris: List<String>)
