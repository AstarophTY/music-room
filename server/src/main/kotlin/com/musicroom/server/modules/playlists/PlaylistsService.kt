package com.musicroom.server.modules.playlists

import com.musicroom.server.common.exceptions.BadRequestException
import com.musicroom.server.common.exceptions.ConflictException
import com.musicroom.server.common.exceptions.ForbiddenException
import com.musicroom.server.common.exceptions.NotFoundException
import com.musicroom.server.common.exceptions.ValidationException
import com.musicroom.server.modules.playlists.entities.Playlist
import com.musicroom.server.modules.premium.HostedTracksService
import com.musicroom.server.modules.spotify.SpotifyService
import com.musicroom.server.modules.users.AvatarImages
import com.musicroom.server.modules.users.UsersRepository
import com.musicroom.server.modules.users.UsersService
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.AuthValidation
import com.musicroom.shared.PlaylistValidation
import com.musicroom.shared.dto.AddPlaylistMemberRequest
import com.musicroom.shared.dto.CreatePlaylistRequest
import com.musicroom.shared.dto.PlaylistAccess
import com.musicroom.shared.dto.PlaylistDetailResponse
import com.musicroom.shared.dto.PlaylistListResponse
import com.musicroom.shared.dto.PlaylistResponse
import com.musicroom.shared.dto.PlaylistType
import com.musicroom.shared.dto.TrackResponse
import com.musicroom.shared.dto.UpdatePlaylistRequest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Music Room playlists. Rights: the owner does everything; members see and play the playlist, and in a shared
 * playlist those with write access also add and remove songs and change the photo. Users who are neither the
 * owner nor a member get 404, so they cannot tell the playlist exists.
 */
class PlaylistsService(
    private val playlists: PlaylistsRepository,
    private val members: PlaylistMembersRepository,
    private val tracks: PlaylistTracksRepository,
    private val users: UsersRepository,
    private val spotify: SpotifyService,
    private val hosted: HostedTracksService,
    private val friends: PlaylistFriends,
) {

    suspend fun list(userId: UUID): PlaylistListResponse =
        PlaylistListResponse(playlists.forUser(userId).mapNotNull { playlist -> role(playlist, userId)?.let { summary(playlist, it) } })

    suspend fun create(userId: UUID, request: CreatePlaylistRequest): PlaylistResponse {
        val playlist = playlists.create(userId, validName(request.name), request.type)
        return summary(playlist, Role.OWNER)
    }

    suspend fun detail(userId: UUID, playlistId: UUID): PlaylistDetailResponse {
        val (playlist, role) = visible(userId, playlistId)
        return PlaylistDetailResponse(summary(playlist, role), tracks.list(playlist.id), members.list(playlist.id))
    }

    suspend fun update(userId: UUID, playlistId: UUID, request: UpdatePlaylistRequest): PlaylistDetailResponse {
        owned(userId, playlistId)
        playlists.update(playlistId, request.name?.let { validName(it) }, request.type)
        return detail(userId, playlistId)
    }

    suspend fun delete(userId: UUID, playlistId: UUID) {
        owned(userId, playlistId)
        playlists.delete(playlistId)
    }

    suspend fun uploadCover(userId: UUID, playlistId: UUID, bytes: ByteArray): PlaylistDetailResponse {
        editable(userId, playlistId)
        if (bytes.size > AvatarImages.MAX_UPLOAD_BYTES) throw UsersService.imageTooLarge()
        val jpeg = withContext(Dispatchers.Default) { AvatarImages.toSquareJpeg(bytes, COVER_SIZE) }
            ?: throw BadRequestException("This file is not a supported image (JPEG or PNG)", ApiErrors.INVALID_IMAGE)
        playlists.saveCover(playlistId, jpeg)
        return detail(userId, playlistId)
    }

    suspend fun deleteCover(userId: UUID, playlistId: UUID): PlaylistDetailResponse {
        editable(userId, playlistId)
        playlists.deleteCover(playlistId)
        return detail(userId, playlistId)
    }

    suspend fun cover(userId: UUID, playlistId: UUID): ByteArray {
        visible(userId, playlistId)
        return playlists.cover(playlistId) ?: throw NotFoundException("No playlist photo")
    }

    /** Reads the track (from Spotify with the user's account, or an uploaded song), then adds it at the end. */
    suspend fun addTrack(userId: UUID, playlistId: UUID, trackId: String): PlaylistDetailResponse {
        editable(userId, playlistId)
        if (trackId.isBlank() || trackId.length > MAX_TRACK_ID_LENGTH) throw NotFoundException("Track not found")
        if (tracks.count(playlistId) >= PlaylistValidation.MAX_TRACKS) {
            throw BadRequestException("A playlist holds ${PlaylistValidation.MAX_TRACKS} songs at most", ApiErrors.PLAYLIST_FULL)
        }
        // A song uploaded to Music Room, or one of the Spotify catalog.
        val track = if (trackId.startsWith(TrackResponse.HOSTED_PREFIX)) hosted.track(trackId) else spotify.track(userId, trackId)
        if (!tracks.add(playlistId, track, userId)) {
            throw ConflictException("This song is already in the playlist", ApiErrors.TRACK_ALREADY_IN_PLAYLIST)
        }
        playlists.touch(playlistId)
        return detail(userId, playlistId)
    }

    suspend fun removeTrack(userId: UUID, playlistId: UUID, entryId: UUID): PlaylistDetailResponse {
        editable(userId, playlistId)
        if (!tracks.remove(playlistId, entryId)) throw NotFoundException("This song is not in the playlist")
        playlists.touch(playlistId)
        return detail(userId, playlistId)
    }

    /** Adds one of the owner's friends ([AddPlaylistMemberRequest.userId]) or a user by email; they get a notification. */
    suspend fun addMember(userId: UUID, playlistId: UUID, request: AddPlaylistMemberRequest): PlaylistDetailResponse {
        val playlist = owned(userId, playlistId)
        checkAccessAllowed(playlist, request.access)
        val friendId = request.userId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        val user = if (request.userId != null) {
            if (friendId == null || !friends.areFriends(userId, friendId)) {
                throw ForbiddenException("You can only invite your friends", ApiErrors.NOT_FRIENDS)
            }
            users.findById(friendId)
        } else {
            users.findByEmail(AuthValidation.normalizeEmail(request.email))
        } ?: throw NotFoundException("No Music Room account uses this email", ApiErrors.USER_NOT_FOUND)
        if (user.id == playlist.ownerId || !members.add(playlistId, user.id, request.access)) {
            throw ConflictException("This user already is in the playlist", ApiErrors.ALREADY_MEMBER)
        }
        friends.memberAdded(playlistId, userId, user.id)
        return detail(userId, playlistId)
    }

    suspend fun updateMember(userId: UUID, playlistId: UUID, memberId: UUID, access: PlaylistAccess): PlaylistDetailResponse {
        checkAccessAllowed(owned(userId, playlistId), access)
        if (!members.update(playlistId, memberId, access)) throw NotFoundException("This user is not in the playlist")
        return detail(userId, playlistId)
    }

    /** The owner removes a member, or a member leaves. */
    suspend fun removeMember(userId: UUID, playlistId: UUID, memberId: UUID) {
        val (_, role) = visible(userId, playlistId)
        if (role != Role.OWNER && memberId != userId) throw ForbiddenException("Only the owner can remove members")
        if (!members.remove(playlistId, memberId)) throw NotFoundException("This user is not in the playlist")
    }

    private fun checkAccessAllowed(playlist: Playlist, access: PlaylistAccess) {
        if (playlist.type == PlaylistType.PERSONAL && access == PlaylistAccess.WRITE) {
            throw BadRequestException(
                "Members of a personal playlist can only listen: make it shared first",
                ApiErrors.PERSONAL_PLAYLIST_READ_ONLY,
            )
        }
    }

    private fun validName(name: String): String {
        PlaylistValidation.nameError(name)?.let { throw ValidationException(it) }
        return name.trim()
    }

    private suspend fun summary(playlist: Playlist, role: Role) = PlaylistResponse(
        id = playlist.id.toString(),
        name = playlist.name,
        type = playlist.type,
        owner = members.user(playlist.ownerId, PlaylistAccess.WRITE) ?: throw NotFoundException("Playlist not found"),
        isOwner = role == Role.OWNER,
        canEdit = role.canEdit(playlist.type),
        trackCount = tracks.count(playlist.id),
        memberCount = members.count(playlist.id),
        coverVersion = playlist.coverUpdatedAt?.toInstant()?.toEpochMilli(),
    )

    private suspend fun role(playlist: Playlist, userId: UUID): Role? = when {
        playlist.ownerId == userId -> Role.OWNER
        else -> when (members.access(playlist.id, userId)) {
            PlaylistAccess.WRITE -> Role.WRITER
            PlaylistAccess.READ -> Role.READER
            null -> null
        }
    }

    private suspend fun visible(userId: UUID, playlistId: UUID): Pair<Playlist, Role> {
        val playlist = playlists.find(playlistId) ?: throw notFound()
        val role = role(playlist, userId) ?: throw notFound()
        return playlist to role
    }

    private suspend fun editable(userId: UUID, playlistId: UUID): Playlist {
        val (playlist, role) = visible(userId, playlistId)
        if (!role.canEdit(playlist.type)) throw ForbiddenException("You can only listen to this playlist")
        return playlist
    }

    private suspend fun owned(userId: UUID, playlistId: UUID): Playlist {
        val (playlist, role) = visible(userId, playlistId)
        if (role != Role.OWNER) throw ForbiddenException("Only the owner can do this")
        return playlist
    }

    private fun notFound() = NotFoundException("Playlist not found")

    /** What the user is to a playlist. A writer only edits a shared playlist. */
    private enum class Role {
        OWNER,
        WRITER,
        READER,
        ;

        fun canEdit(type: PlaylistType) = this == OWNER || (this == WRITER && type == PlaylistType.SHARED)
    }

    private companion object {
        const val COVER_SIZE = 512
        const val MAX_TRACK_ID_LENGTH = 64
    }
}
