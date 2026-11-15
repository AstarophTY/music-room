package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Adds a track using its existing ID and, when available, its explicit catalog identity. */
@Serializable
data class AddPlaylistTrackRequest(val trackId: String, val catalogId: CatalogId? = null)
