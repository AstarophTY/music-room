package com.musicroom.server.config

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val accessTokenTtlMinutes: Long = 15,
    val refreshTokenTtlDays: Long = 30,
) {
    init {
        require(secret.length >= 32) { "JWT_SECRET must be at least 32 characters long" }
    }
}
