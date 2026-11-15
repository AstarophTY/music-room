package com.musicroom.server.modules.catalog

import com.musicroom.server.common.exceptions.BadRequestException
import com.musicroom.shared.dto.CatalogId
import com.musicroom.shared.dto.TrackSource
import com.musicroom.shared.dto.TrackResponse
import java.util.UUID

/** Resolves catalog identifiers through the provider that owns them. */
class TrackCatalog(private val providers: List<Provider>) {
    interface Provider {
        val source: TrackSource
        suspend fun track(userId: UUID, id: CatalogId): TrackResponse
    }

    suspend fun track(userId: UUID, id: CatalogId): TrackResponse =
        (providers.firstOrNull { it.source == id.source } ?: throw BadRequestException("Unsupported catalog provider"))
            .track(userId, id)

}
