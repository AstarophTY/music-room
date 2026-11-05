package com.musicroom.server.config

import io.ktor.server.config.ApplicationConfig

data class AppConfig(
    val version: String,
    val database: DatabaseConfig,
    val jwt: JwtConfig,
    val oauth: OAuthConfig = OAuthConfig(),
    val mail: MailConfig = MailConfig(),
    val spotify: SpotifyConfig = SpotifyConfig(),
    val stripe: StripeConfig = StripeConfig(),
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
                refreshTokenTtlDays = config.property("jwt.refreshTokenTtlDays").getString().toLong(),
            ),
            oauth = OAuthConfig(
                googleClientIds = config.list("oauth.googleClientIds"),
                appleClientIds = config.list("oauth.appleClientIds"),
            ),
            mail = mailConfig(config),
            stripe = StripeConfig(
                secretKey = config.string("stripe.secretKey"),
                webhookSecret = config.string("stripe.webhookSecret"),
                priceId = config.string("stripe.priceId"),
                returnUrl = config.string("stripe.returnUrl").ifEmpty { StripeConfig().returnUrl },
                automaticTax = config.string("stripe.automaticTax").toBoolean(),
            ),
            spotify = SpotifyConfig(
                clientId = config.string("spotify.clientId"),
                clientSecret = config.string("spotify.clientSecret"),
                redirectUri = config.string("spotify.redirectUri").ifEmpty { SpotifyConfig().redirectUri },
            ),
        )

        /** No SMTP_HOST: development mode, every email is written to the server logs. */
        private fun mailConfig(config: ApplicationConfig): MailConfig {
            val from = config.string("mail.from").ifEmpty { MailConfig().from }
            val host = config.string("mail.host")
            if (host.isEmpty()) return MailConfig(from = from)
            return MailConfig(
                host = host,
                port = config.string("mail.port").toIntOrNull() ?: SMTP_SUBMISSION_PORT,
                user = config.string("mail.user"),
                password = config.string("mail.password"),
                from = from,
            )
        }

        private const val SMTP_SUBMISSION_PORT = 587

        private fun ApplicationConfig.string(path: String): String =
            propertyOrNull(path)?.getString()?.trim().orEmpty()

        /** Comma-separated value, e.g. "id-1,id-2". */
        private fun ApplicationConfig.list(path: String): List<String> =
            string(path).split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }
}
