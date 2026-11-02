package com.musicroom.server.config

import io.ktor.server.config.ApplicationConfig

data class AppConfig(
    val version: String,
    val database: DatabaseConfig,
    val jwt: JwtConfig,
) {
    companion object {
        fun from(config: ApplicationConfig) = AppConfig(
            version = config.property("app.version").getString(),
            database = DatabaseConfig(
                url = config.property("database.url").getString(),
                user = config.property("database.user").getString(),
                password = config.property("database.password").getString(),
                maxPoolSize = config.property("database.maxPoolSize").getString().toInt(),
            ),
            jwt = JwtConfig(
                secret = config.property("jwt.secret").getString(),
                issuer = config.property("jwt.issuer").getString(),
                audience = config.property("jwt.audience").getString(),
                realm = config.property("jwt.realm").getString(),
                accessTokenTtlMinutes = config.property("jwt.accessTokenTtlMinutes").getString().toLong(),
            ),
        )
    }
}

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int = 10,
)

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val accessTokenTtlMinutes: Long = 15,
) {
    init {
        require(secret.length >= 32) { "JWT_SECRET must be at least 32 characters long" }
    }
}
