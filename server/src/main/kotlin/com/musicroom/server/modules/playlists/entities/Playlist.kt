package com.musicroom.server.modules.playlists.entities

import com.musicroom.shared.dto.PlaylistEditLicense
import com.musicroom.shared.dto.PlaylistVisibility
import java.time.OffsetDateTime
import java.util.UUID

data class Playlist(
    val id: UUID,
    val ownerId: UUID,
    val name: String,
    val visibility: PlaylistVisibility,
    val editLicense: PlaylistEditLicense,
    val revision: Long,
    val coverUpdatedAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime,
)
