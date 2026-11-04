package com.musicroom.server.modules.playlists.entities

import com.musicroom.shared.dto.PlaylistType
import java.time.OffsetDateTime
import java.util.UUID

data class Playlist(
    val id: UUID,
    val ownerId: UUID,
    val name: String,
    val type: PlaylistType,
    val coverUpdatedAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime,
)
