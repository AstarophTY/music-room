package com.musicroom.server

import com.musicroom.server.common.Module
import com.musicroom.server.common.filters.configureExceptionFilter
import com.musicroom.server.common.guards.configureJwtGuard
import com.musicroom.server.common.interceptors.configureLoggingInterceptor
import com.musicroom.server.common.jwt.JwtService
import com.musicroom.server.common.middleware.configureHttpMiddleware
import com.musicroom.server.common.register
import com.musicroom.server.common.serialization.configureSerialization
import com.musicroom.server.common.throttler.configureThrottler
import com.musicroom.server.common.websocket.configureWebSockets
import com.musicroom.server.config.AppConfig
import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.system.SystemModule
import com.musicroom.server.modules.users.UsersModule
import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing

class AppModule(config: AppConfig, database: DatabaseModule) {
    val jwtService = JwtService(config.jwt)

    val modules: List<Module> = listOf(
        SystemModule(config, database),
        UsersModule(database),
    )
}

fun Application.bootstrap(appModule: AppModule) {
    configureLoggingInterceptor()
    configureSerialization()
    configureHttpMiddleware()
    configureExceptionFilter()
    configureJwtGuard(appModule.jwtService)
    configureThrottler()
    configureWebSockets()

    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        appModule.modules.flatMap { it.controllers }.forEach { register(it) }
    }
}
