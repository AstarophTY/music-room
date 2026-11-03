package com.musicroom.server.modules.users

import com.musicroom.server.common.Controller
import com.musicroom.server.common.exceptions.NotFoundException
import com.musicroom.server.common.guards.currentUserId
import com.musicroom.server.common.guards.jwtGuard
import com.musicroom.shared.ApiRoutes
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.server.request.contentLength
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.cacheControl
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.put
import io.ktor.utils.io.readRemaining
import io.ktor.server.request.receive
import kotlinx.io.readByteArray
import java.util.UUID

class UsersController(private val service: UsersService) : Controller {
    override fun Route.routes() {
        jwtGuard {
            get(ApiRoutes.USERS_ME) {
                call.respond(service.getById(call.currentUserId()).toResponse())
            }
            patch(ApiRoutes.USERS_ME) {
                call.respond(service.updateProfile(call.currentUserId(), call.receive()).toResponse())
            }
            put(ApiRoutes.USERS_ME_AVATAR) {
                val declared = call.request.contentLength()
                if (declared != null && declared > AvatarImages.MAX_UPLOAD_BYTES) throw UsersService.imageTooLarge()
                // Read one byte more than allowed: a bigger body without Content-Length is refused too.
                val bytes = call.receiveChannel().readRemaining(AvatarImages.MAX_UPLOAD_BYTES + 1L).readByteArray()
                call.respond(service.uploadAvatar(call.currentUserId(), bytes).toResponse())
            }
            delete(ApiRoutes.USERS_ME_AVATAR) {
                call.respond(service.deleteAvatar(call.currentUserId()).toResponse())
            }
            get(ApiRoutes.USER_AVATAR) {
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: throw NotFoundException("No profile photo")
                call.response.cacheControl(CacheControl.MaxAge(maxAgeSeconds = 3600, visibility = CacheControl.Visibility.Private))
                call.respondBytes(service.avatar(id), ContentType.Image.JPEG)
            }
        }
    }
}
