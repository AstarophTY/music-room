package com.musicroom.server.modules.applemusic

import com.musicroom.server.common.Controller
import com.musicroom.server.common.guards.jwtGuard
import com.musicroom.shared.ApiRoutes
import com.musicroom.shared.dto.AppleMusicDeveloperToken
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import com.musicroom.server.common.exceptions.BadRequestException

class AppleMusicController(private val tokens: AppleMusicTokenService, private val catalog: AppleMusicCatalog) : Controller {
    override fun Route.routes() {
        jwtGuard {
            get(ApiRoutes.APPLE_MUSIC_DEVELOPER_TOKEN) {
                call.respond(AppleMusicDeveloperToken(tokens.create()))
            }
            get(ApiRoutes.APPLE_MUSIC_SEARCH) {
                val query = call.request.queryParameters["q"] ?: throw BadRequestException("Missing search query")
                val storefront = call.request.queryParameters["storefront"] ?: throw BadRequestException("Missing storefront")
                call.respond(catalog.search(query, storefront))
            }
            get(ApiRoutes.APPLE_MUSIC_CHARTS) {
                val storefront = call.request.queryParameters["storefront"] ?: throw BadRequestException("Missing storefront")
                call.respond(catalog.charts(storefront))
            }
        }
    }
}
