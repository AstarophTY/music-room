package com.musicroom.server.modules.applemusic

import com.musicroom.server.common.Controller
import com.musicroom.server.common.guards.jwtGuard
import com.musicroom.shared.ApiRoutes
import com.musicroom.shared.dto.AppleMusicDeveloperToken
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

class AppleMusicController(private val tokens: AppleMusicTokenService) : Controller {
    override fun Route.routes() {
        jwtGuard {
            get(ApiRoutes.APPLE_MUSIC_DEVELOPER_TOKEN) {
                call.respond(AppleMusicDeveloperToken(tokens.create()))
            }
        }
    }
}
