package com.musicroom.server.database

import com.musicroom.server.config.DatabaseConfig
import org.testcontainers.containers.PostgreSQLContainer

object TestDatabase {
    private val container by lazy {
        PostgreSQLContainer("postgres:17-alpine").also { it.start() }
    }

    val config: DatabaseConfig by lazy {
        DatabaseConfig(container.jdbcUrl, container.username, container.password)
    }

    val module: DatabaseModule by lazy {
        DatabaseModule.connect(config)
    }
}
