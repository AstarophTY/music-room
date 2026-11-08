package com.musicroom.server.modules.catalog

import com.musicroom.shared.dto.TrackResponse
import java.util.UUID

/** Resolves catalog identifiers through the provider that owns them. */
class TrackCatalog(private val providers: List<Provider>) {
    interface Provider {
        fun accepts(id: String): Boolean
        suspend fun track(userId: UUID, id: String): TrackResponse
    }

    suspend fun track(userId: UUID, id: String): TrackResponse =
        providers.first { it.accepts(id) }.track(userId, id)

}
