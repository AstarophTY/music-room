package com.musicroom.server.modules.system

import com.musicroom.server.common.Controller
import com.musicroom.server.common.Module
import com.musicroom.server.config.AppConfig
import com.musicroom.server.database.DatabaseModule

class SystemModule(config: AppConfig, database: DatabaseModule) : Module {
    private val service = SystemService(version = config.version, isDatabaseUp = database::isHealthy)

    override val controllers: List<Controller> = listOf(SystemController(service))
}
