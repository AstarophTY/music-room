package com.musicroom.server.modules.users

import com.musicroom.server.common.Controller
import com.musicroom.server.common.guards.currentUserId
import com.musicroom.server.common.guards.jwtGuard
import com.musicroom.shared.ApiRoutes
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch

class UsersController(private val service: UsersService) : Controller {
    override fun Route.routes() {
        jwtGuard {
            get(ApiRoutes.USERS_ME) {
                call.respond(service.getById(call.currentUserId()).toResponse())
            }
            patch(ApiRoutes.USERS_ME) {
                call.respond(service.updateProfile(call.currentUserId(), call.receive()).toResponse())
            }
        }
    }
}
