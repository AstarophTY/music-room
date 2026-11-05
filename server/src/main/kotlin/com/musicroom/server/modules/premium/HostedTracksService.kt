package com.musicroom.server.modules.premium

import com.musicroom.server.common.exceptions.BadRequestException
import com.musicroom.server.common.exceptions.ConflictException
import com.musicroom.server.common.exceptions.ForbiddenException
import com.musicroom.server.common.exceptions.HttpException
import com.musicroom.server.common.exceptions.NotFoundException
import com.musicroom.server.modules.users.UsersRepository
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.dto.HostedTracksResponse
import com.musicroom.shared.dto.TrackResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Songs uploaded to Music Room. Only Premium users upload (MP3 files, up to [MAX_UPLOAD_BYTES] and [MAX_PER_USER]
 * songs); everyone signed in can list and play them, as they show on their owner's profile and in playlists. They
 * stay when Premium ends; their owner can still delete them.
 */
class HostedTracksService(
    private val tracks: HostedTracksRepository,
    private val billing: BillingService,
    private val users: UsersRepository,
) {

    suspend fun upload(userId: UUID, audio: ByteArray, title: String?, artist: String?): TrackResponse {
        if (!billing.isPremium(userId)) throw ForbiddenException("Uploading songs needs Music Room Premium", ApiErrors.PREMIUM_REQUIRED)
        if (audio.size > MAX_UPLOAD_BYTES) throw tooLarge()
        val name = title?.trim().orEmpty().take(MAX_TEXT)
        if (name.isEmpty()) throw BadRequestException("Give the song a title")
        val by = artist?.trim().orEmpty().take(MAX_TEXT).ifEmpty { users.findById(userId)?.displayName.orEmpty() }
        if (tracks.count(userId) >= MAX_PER_USER) {
            throw ConflictException("You can upload up to $MAX_PER_USER songs", ApiErrors.HOSTED_TRACK_LIMIT)
        }
        val duration = withContext(Dispatchers.Default) { Mp3Audio.durationMs(audio) }
            ?: throw BadRequestException("This file is not an MP3", ApiErrors.INVALID_AUDIO)
        return tracks.create(userId, name, by, duration, audio).toResponse()
    }

    suspend fun mine(userId: UUID): HostedTracksResponse = HostedTracksResponse(tracks.byOwner(userId).map { it.toResponse() })

    suspend fun byUser(userId: UUID): HostedTracksResponse {
        if (users.findById(userId) == null) throw NotFoundException("User not found")
        return mine(userId)
    }

    /** The song [trackId] (a TrackResponse id, "mr-<uuid>"), for playlists. */
    suspend fun track(trackId: String): TrackResponse = (parse(trackId)?.let { tracks.find(it) } ?: throw notFound()).toResponse()

    suspend fun audio(id: UUID): ByteArray = tracks.audio(id) ?: throw notFound()

    suspend fun delete(userId: UUID, id: UUID) {
        val track = tracks.find(id) ?: throw notFound()
        if (track.ownerId != userId) throw notFound()
        tracks.delete(id)
    }

    private fun parse(trackId: String): UUID? =
        trackId.removePrefix(TrackResponse.HOSTED_PREFIX).takeIf { trackId.startsWith(TrackResponse.HOSTED_PREFIX) }
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    private fun notFound() = NotFoundException("Song not found")

    companion object {
        const val MAX_UPLOAD_BYTES = 20 * 1024 * 1024
        const val MAX_PER_USER = 200
        private const val MAX_TEXT = 100

        fun tooLarge() = HttpException(HttpStatusCode.PayloadTooLarge, ApiErrors.AUDIO_TOO_LARGE, "The file is too big (20 MB at most)")
    }
}
