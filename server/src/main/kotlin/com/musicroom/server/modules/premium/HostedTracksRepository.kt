package com.musicroom.server.modules.premium

import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.playlists.entities.PlaylistTracksTable
import com.musicroom.server.modules.premium.entities.HostedTrack
import com.musicroom.server.modules.premium.entities.HostedTrackAudioTable
import com.musicroom.server.modules.premium.entities.HostedTracksTable
import com.musicroom.shared.dto.TrackResponse
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.OffsetDateTime
import java.util.UUID

/** Songs uploaded by Premium users and their MP3 files. */
class HostedTracksRepository(private val db: DatabaseModule) {

    suspend fun create(ownerId: UUID, title: String, artist: String, durationMs: Long, audio: ByteArray): HostedTrack = db.query {
        val id = HostedTracksTable.insertAndGetId {
            it[HostedTracksTable.ownerId] = ownerId
            it[HostedTracksTable.title] = title
            it[HostedTracksTable.artist] = artist
            it[HostedTracksTable.durationMs] = durationMs
            it[sizeBytes] = audio.size
            it[createdAt] = OffsetDateTime.now()
        }.value
        HostedTrackAudioTable.insert {
            it[trackId] = id
            it[HostedTrackAudioTable.audio] = audio
        }
        HostedTrack(id, ownerId, title, artist, durationMs)
    }

    suspend fun find(id: UUID): HostedTrack? = db.query {
        HostedTracksTable.selectAll().where { HostedTracksTable.id eq id }.singleOrNull()?.toHostedTrack()
    }

    /** The songs of [ownerId], newest first. */
    suspend fun byOwner(ownerId: UUID): List<HostedTrack> = db.query {
        HostedTracksTable.selectAll().where { HostedTracksTable.ownerId eq ownerId }
            .orderBy(HostedTracksTable.createdAt, SortOrder.DESC)
            .map { it.toHostedTrack() }
    }

    suspend fun count(ownerId: UUID): Int = db.query {
        HostedTracksTable.selectAll().where { HostedTracksTable.ownerId eq ownerId }.count().toInt()
    }

    suspend fun audio(id: UUID): ByteArray? = db.query {
        HostedTrackAudioTable.selectAll().where { HostedTrackAudioTable.trackId eq id }.singleOrNull()?.get(HostedTrackAudioTable.audio)
    }

    /** Deletes the song, its file, and its places in the playlists. */
    suspend fun delete(id: UUID) {
        db.query {
            PlaylistTracksTable.deleteWhere { PlaylistTracksTable.spotifyId eq TrackResponse.HOSTED_PREFIX + id }
            HostedTracksTable.deleteWhere { HostedTracksTable.id eq id }
        }
    }

    private fun ResultRow.toHostedTrack() = HostedTrack(
        id = this[HostedTracksTable.id].value,
        ownerId = this[HostedTracksTable.ownerId].value,
        title = this[HostedTracksTable.title],
        artist = this[HostedTracksTable.artist],
        durationMs = this[HostedTracksTable.durationMs],
    )
}
