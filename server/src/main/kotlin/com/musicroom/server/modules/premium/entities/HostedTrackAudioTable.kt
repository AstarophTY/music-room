package com.musicroom.server.modules.premium.entities

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object HostedTrackAudioTable : Table("hosted_track_audio") {
    val trackId = reference("track_id", HostedTracksTable, onDelete = ReferenceOption.CASCADE)
    val audio = binary("audio")

    override val primaryKey = PrimaryKey(trackId)
}
