package com.musicroom.server.config

/** SMTP server used to send emails. The defaults match the Mailpit container from docker-compose.yml. */
data class MailConfig(
    val host: String = "localhost",
    val port: Int = 1025,
    val user: String = "",
    val password: String = "",
    val from: String = "Music Room <no-reply@musicroom.local>",
)
