package com.musicroom.server.modules.playlists

import com.musicroom.server.common.exceptions.BadRequestException
import com.musicroom.server.common.exceptions.ConflictException
import com.musicroom.server.common.exceptions.ForbiddenException
import com.musicroom.server.common.exceptions.NotFoundException
import com.musicroom.server.common.exceptions.ValidationException
import com.musicroom.server.modules.playlists.entities.Playlist
import com.musicroom.server.modules.premium.HostedTracksService
import com.musicroom.server.modules.catalog.TrackCatalog
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
import com.musicroom.shared.dto.PlaylistEditLicense
import com.musicroom.shared.dto.PlaylistResponse
import com.musicroom.shared.dto.PlaylistVisibility
import com.musicroom.shared.dto.CatalogId
import com.musicroom.shared.dto.TrackSource
import com.musicroom.shared.dto.TrackResponse
import com.musicroom.shared.dto.UpdatePlaylistRequest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Music Room playlists. A public playlist is open to every user; a private one only to its owner and invited
 * members, and anyone else gets 404, so they cannot tell it exists. With the "everyone" license, whoever can open
 * the playlist edits it; with the "invited" license, only the owner and the members with write access do.
 */
class PlaylistsService(
    private val playlists: PlaylistsRepository,
    private val members: PlaylistMembersRepository,
    private val tracks: PlaylistTracksRepository,
    private val users: UsersRepository,
    private val catalog: TrackCatalog,
    private val hosted: HostedTracksService,
    private val friends: PlaylistFriends,
    publishChange: (suspend (UUID) -> Unit)? = null,
) {
    val hub = PlaylistsHub(::render, publishChange)

    suspend fun list(userId: UUID): PlaylistListResponse =
        PlaylistListResponse(playlists.forUser(userId).mapNotNull { playlist -> role(playlist, userId)?.let { summary(playlist, it) } })

    /** Public playlists the user can open, whether or not they are in them. */
    suspend fun listPublic(userId: UUID, query: String): PlaylistListResponse =
        PlaylistListResponse(playlists.public(query.take(PlaylistValidation.NAME_MAX_LENGTH), PUBLIC_PAGE).mapNotNull { playlist ->
            role(playlist, userId)?.let { summary(playlist, it) }
        })

    suspend fun create(userId: UUID, request: CreatePlaylistRequest): PlaylistResponse {
        val playlist = playlists.create(userId, validName(request.name), request.visibility, request.editLicense)
        return summary(playlist, Role.OWNER)
    }

    suspend fun detail(userId: UUID, playlistId: UUID): PlaylistDetailResponse {
        val (playlist, role) = visible(userId, playlistId)
        return PlaylistDetailResponse(summary(playlist, role), tracks.list(playlist.id), members.list(playlist.id))
    }

    suspend fun update(userId: UUID, playlistId: UUID, request: UpdatePlaylistRequest): PlaylistDetailResponse {
        owned(userId, playlistId)
        playlists.update(playlistId, request.name?.let { validName(it) }, request.visibility, request.editLicense)
        return changed(userId, playlistId)
    }

    suspend fun delete(userId: UUID, playlistId: UUID) {
        owned(userId, playlistId)
        playlists.delete(playlistId)
        hub.changed(playlistId)
    }

    suspend fun uploadCover(userId: UUID, playlistId: UUID, bytes: ByteArray): PlaylistDetailResponse {
        editable(userId, playlistId)
        if (bytes.size > AvatarImages.MAX_UPLOAD_BYTES) throw UsersService.imageTooLarge()
        val jpeg = withContext(Dispatchers.Default) { AvatarImages.toSquareJpeg(bytes, COVER_SIZE) }
            ?: throw BadRequestException("This file is not a supported image (JPEG or PNG)", ApiErrors.INVALID_IMAGE)
        playlists.saveCover(playlistId, jpeg)
        return changed(userId, playlistId)
    }

    suspend fun deleteCover(userId: UUID, playlistId: UUID): PlaylistDetailResponse {
        editable(userId, playlistId)
        playlists.deleteCover(playlistId)
        return changed(userId, playlistId)
    }

    suspend fun cover(userId: UUID, playlistId: UUID): ByteArray {
        visible(userId, playlistId)
        return playlists.cover(playlistId) ?: throw NotFoundException("No playlist photo")
    }

    /** Reads the track (from Spotify with the user's account, or an uploaded song), then adds it at the end. */
    suspend fun addTrack(userId: UUID, playlistId: UUID, trackId: String, identity: CatalogId? = null): PlaylistDetailResponse {
        editable(userId, playlistId)
        if (trackId.isBlank() || trackId.length > MAX_TRACK_ID_LENGTH) throw NotFoundException("Track not found")
        if (tracks.count(playlistId) >= PlaylistValidation.MAX_TRACKS) {
            throw BadRequestException("A playlist holds ${PlaylistValidation.MAX_TRACKS} songs at most", ApiErrors.PLAYLIST_FULL)
        }
        val parsed = CatalogId.fromLegacy(trackId) ?: throw BadRequestException("Invalid catalog identifier")
        if (identity != null && identity != parsed) throw BadRequestException("Catalog identifier does not match track ID")
        val track = when (parsed.source) {
            TrackSource.MUSIC_ROOM -> hosted.track(trackId)
            else -> catalog.track(userId, parsed)
        }
        if (!tracks.add(playlistId, track, userId)) {
            throw ConflictException("This song is already in the playlist", ApiErrors.TRACK_ALREADY_IN_PLAYLIST)
        }
        return changed(userId, playlistId)
    }

    /** Moves a song right after [afterEntryId], or to the top when it is null. */
    suspend fun moveTrack(userId: UUID, playlistId: UUID, entryId: UUID, afterEntryId: UUID?): PlaylistDetailResponse {
        editable(userId, playlistId)
        tracks.move(playlistId, entryId, afterEntryId)
        return changed(userId, playlistId)
    }

    suspend fun removeTrack(userId: UUID, playlistId: UUID, entryId: UUID): PlaylistDetailResponse {
        editable(userId, playlistId)
        if (!tracks.remove(playlistId, entryId)) throw NotFoundException("This song is not in the playlist")
        return changed(userId, playlistId)
    }

    /** Adds one of the owner's friends ([AddPlaylistMemberRequest.userId]) or a user by email; they get a notification. */
    suspend fun addMember(userId: UUID, playlistId: UUID, request: AddPlaylistMemberRequest): PlaylistDetailResponse {
        val playlist = owned(userId, playlistId)
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
        return changed(userId, playlistId)
    }

    suspend fun updateMember(userId: UUID, playlistId: UUID, memberId: UUID, access: PlaylistAccess): PlaylistDetailResponse {
        owned(userId, playlistId)
        if (!members.update(playlistId, memberId, access)) throw NotFoundException("This user is not in the playlist")
        return changed(userId, playlistId)
    }

    /** The owner removes a member, or a member leaves. */
    suspend fun removeMember(userId: UUID, playlistId: UUID, memberId: UUID) {
        val (_, role) = visible(userId, playlistId)
        if (role != Role.OWNER && memberId != userId) throw ForbiddenException("Only the owner can remove members")
        if (!members.remove(playlistId, memberId)) throw NotFoundException("This user is not in the playlist")
        playlists.touch(playlistId)
        hub.changed(playlistId)
    }

    /** Records a change, tells the people following the playlist and answers with its new state. */
    private suspend fun changed(userId: UUID, playlistId: UUID): PlaylistDetailResponse {
        playlists.touch(playlistId)
        hub.changed(playlistId)
        return detail(userId, playlistId)
    }

    /** The playlist as each of [userIds] sees it, read once for all of them. Those who cannot see it are left out. */
    private suspend fun render(playlistId: UUID, userIds: Set<UUID>): Map<UUID, PlaylistDetailResponse> {
        val playlist = playlists.find(playlistId) ?: return emptyMap()
        val songs = tracks.list(playlistId)
        val people = members.list(playlistId)
        val base = summary(playlist, Role.GUEST)
        val accessOf = people.associate { it.userId to it.access }
        return userIds.mapNotNull { userId ->
            val role = roleOf(playlist, userId, accessOf[userId.toString()]) ?: return@mapNotNull null
            val view = base.copy(isOwner = role == Role.OWNER, canEdit = role.canEdit(playlist.editLicense))
            userId to PlaylistDetailResponse(view, songs, people)
        }.toMap()
    }

    private fun validName(name: String): String {
        PlaylistValidation.nameError(name)?.let { throw ValidationException(it) }
        return name.trim()
    }

    private suspend fun summary(playlist: Playlist, role: Role) = PlaylistResponse(
        id = playlist.id.toString(),
        name = playlist.name,
        visibility = playlist.visibility,
        editLicense = playlist.editLicense,
        owner = members.user(playlist.ownerId, PlaylistAccess.WRITE) ?: throw NotFoundException("Playlist not found"),
        isOwner = role == Role.OWNER,
        canEdit = role.canEdit(playlist.editLicense),
        trackCount = tracks.count(playlist.id),
        memberCount = members.count(playlist.id),
        coverVersion = playlist.coverUpdatedAt?.toInstant()?.toEpochMilli(),
        revision = playlist.revision,
    )

    private suspend fun role(playlist: Playlist, userId: UUID): Role? =
        roleOf(playlist, userId, if (playlist.ownerId == userId) null else members.access(playlist.id, userId))

    private fun roleOf(playlist: Playlist, userId: UUID, access: PlaylistAccess?): Role? = when {
        playlist.ownerId == userId -> Role.OWNER
        access == PlaylistAccess.WRITE -> Role.WRITER
        access == PlaylistAccess.READ -> Role.READER
        playlist.visibility == PlaylistVisibility.PUBLIC -> Role.GUEST
        else -> null
    }

    private suspend fun visible(userId: UUID, playlistId: UUID): Pair<Playlist, Role> {
        val playlist = playlists.find(playlistId) ?: throw notFound()
        val role = role(playlist, userId) ?: throw notFound()
        return playlist to role
    }

    private suspend fun editable(userId: UUID, playlistId: UUID): Playlist {
        val (playlist, role) = visible(userId, playlistId)
        if (!role.canEdit(playlist.editLicense)) throw ForbiddenException("You can only listen to this playlist")
        return playlist
    }

    private suspend fun owned(userId: UUID, playlistId: UUID): Playlist {
        val (playlist, role) = visible(userId, playlistId)
        if (role != Role.OWNER) throw ForbiddenException("Only the owner can do this")
        return playlist
    }

    private fun notFound() = NotFoundException("Playlist not found")

    /** What the user is to a playlist. [GUEST]: not invited, but the playlist is public. */
    private enum class Role {
        OWNER,
        WRITER,
        READER,
        GUEST,
        ;

        fun canEdit(license: PlaylistEditLicense) = this == OWNER || this == WRITER || license == PlaylistEditLicense.EVERYONE
    }

    private companion object {
        const val COVER_SIZE = 512
        const val MAX_TRACK_ID_LENGTH = 64
        const val PUBLIC_PAGE = 50
    }
}
