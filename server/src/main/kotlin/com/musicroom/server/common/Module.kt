package com.musicroom.server.common

import io.ktor.server.routing.Route

interface Controller {
    fun Route.routes()
}

interface Module {
    val controllers: List<Controller>
}

fun Route.register(controller: Controller) = with(controller) { routes() }
