package com.musicroom.server.modules.playlists

import com.musicroom.server.common.Controller
import com.musicroom.server.common.exceptions.NotFoundException
import com.musicroom.server.common.guards.currentUserId
import com.musicroom.server.common.guards.jwtGuard
import com.musicroom.server.modules.users.AvatarImages
import com.musicroom.server.modules.users.UsersService
import com.musicroom.shared.ApiRoutes
import com.musicroom.shared.dto.AddPlaylistMemberRequest
import com.musicroom.shared.dto.AddPlaylistTrackRequest
import com.musicroom.shared.dto.CreatePlaylistRequest
import com.musicroom.shared.dto.UpdatePlaylistMemberRequest
import com.musicroom.shared.dto.UpdatePlaylistRequest
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.contentLength
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.cacheControl
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.utils.io.readRemaining
import java.util.UUID
import kotlinx.io.readByteArray

class PlaylistsController(private val service: PlaylistsService) : Controller {
    override fun Route.routes() {
        jwtGuard {
            get(ApiRoutes.PLAYLISTS) {
                call.respond(service.list(call.currentUserId()))
            }
            post(ApiRoutes.PLAYLISTS) {
                call.respond(HttpStatusCode.Created, service.create(call.currentUserId(), call.receive<CreatePlaylistRequest>()))
            }
            get(ApiRoutes.PLAYLIST) {
                call.respond(service.detail(call.currentUserId(), call.uuid("id")))
            }
            patch(ApiRoutes.PLAYLIST) {
                call.respond(service.update(call.currentUserId(), call.uuid("id"), call.receive<UpdatePlaylistRequest>()))
            }
            delete(ApiRoutes.PLAYLIST) {
                service.delete(call.currentUserId(), call.uuid("id"))
                call.respond(HttpStatusCode.NoContent)
            }

            put(ApiRoutes.PLAYLIST_COVER) {
                val declared = call.request.contentLength()
                if (declared != null && declared > AvatarImages.MAX_UPLOAD_BYTES) throw UsersService.imageTooLarge()
                // Read one byte more than allowed: a bigger body without Content-Length is refused too.
                val bytes = call.receiveChannel().readRemaining(AvatarImages.MAX_UPLOAD_BYTES + 1L).readByteArray()
                call.respond(service.uploadCover(call.currentUserId(), call.uuid("id"), bytes))
            }
            delete(ApiRoutes.PLAYLIST_COVER) {
                call.respond(service.deleteCover(call.currentUserId(), call.uuid("id")))
            }
            get(ApiRoutes.PLAYLIST_COVER) {
                val cover = service.cover(call.currentUserId(), call.uuid("id"))
                call.response.cacheControl(CacheControl.MaxAge(maxAgeSeconds = 3600, visibility = CacheControl.Visibility.Private))
                call.respondBytes(cover, ContentType.Image.JPEG)
            }

            post(ApiRoutes.PLAYLIST_TRACKS) {
                val request = call.receive<AddPlaylistTrackRequest>()
                call.respond(service.addTrack(call.currentUserId(), call.uuid("id"), request.trackId.trim(), request.catalogId))
            }
            delete(ApiRoutes.PLAYLIST_TRACK) {
                call.respond(service.removeTrack(call.currentUserId(), call.uuid("id"), call.uuid("entryId")))
            }

            post(ApiRoutes.PLAYLIST_MEMBERS) {
                call.respond(service.addMember(call.currentUserId(), call.uuid("id"), call.receive<AddPlaylistMemberRequest>()))
            }
            patch(ApiRoutes.PLAYLIST_MEMBER) {
                val request = call.receive<UpdatePlaylistMemberRequest>()
                call.respond(service.updateMember(call.currentUserId(), call.uuid("id"), call.uuid("userId"), request.access))
            }
            delete(ApiRoutes.PLAYLIST_MEMBER) {
                service.removeMember(call.currentUserId(), call.uuid("id"), call.uuid("userId"))
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }

    /** A malformed id cannot match anything: 404, like an unknown one. */
    private fun ApplicationCall.uuid(name: String): UUID =
        parameters[name]?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: throw NotFoundException("Not found")
}
