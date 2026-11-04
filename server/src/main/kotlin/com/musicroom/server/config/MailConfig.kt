package com.musicroom.server.config

/** SMTP server used to send emails. An empty [host] means development: emails are only logged. */
data class MailConfig(
    val host: String = "",
    val port: Int = 587,
    val user: String = "",
    val password: String = "",
    val from: String = "Music Room <no-reply@musicroom.local>",
)
