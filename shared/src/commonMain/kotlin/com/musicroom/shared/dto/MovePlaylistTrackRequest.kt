package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/**
 * Moves a song right after the entry [afterEntryId], or to the top when it is null. Naming a neighbour instead of an
 * index keeps the intent when other people move songs at the same time.
 */
@Serializable
data class MovePlaylistTrackRequest(val afterEntryId: String? = null)
