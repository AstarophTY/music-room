package com.musicroom.server

import com.musicroom.server.config.AppConfig
import com.musicroom.server.database.DatabaseModule
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped

fun Application.module() {
    val config = AppConfig.from(environment.config)
    val database = DatabaseModule.connect(config.database)
    monitor.subscribe(ApplicationStopped) { database.close() }

    bootstrap(AppModule(config, database))
}
