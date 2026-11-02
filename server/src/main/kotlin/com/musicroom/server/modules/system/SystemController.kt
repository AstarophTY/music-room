package com.musicroom.server.modules.system

import com.musicroom.server.common.Controller
import com.musicroom.shared.ApiRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

class SystemController(private val service: SystemService) : Controller {
    override fun Route.routes() {
        get(ApiRoutes.HEALTH) {
            val health = service.health()
            val status = if (health.database == "up") HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
            call.respond(status, health)
        }
        get(ApiRoutes.INFO) {
            call.respond(service.info())
        }
    }
}
