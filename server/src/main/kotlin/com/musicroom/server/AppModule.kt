package com.musicroom.server

import com.musicroom.server.common.Module
import com.musicroom.server.common.filters.configureExceptionFilter
import com.musicroom.server.common.guards.configureJwtGuard
import com.musicroom.server.common.interceptors.configureLoggingInterceptor
import com.musicroom.server.common.jwt.JwtService
import com.musicroom.server.common.mail.EmailSender
import com.musicroom.server.common.mail.LogEmailSender
import com.musicroom.server.common.mail.SmtpEmailSender
import com.musicroom.server.common.middleware.configureHttpMiddleware
import com.musicroom.server.common.oauth.JwksOAuthTokenVerifier
import com.musicroom.server.common.oauth.OAuthTokenVerifier
import com.musicroom.server.common.register
import com.musicroom.server.common.security.PasswordHasher
import com.musicroom.server.common.security.SecretBox
import com.musicroom.server.common.serialization.configureSerialization
import com.musicroom.server.common.throttler.configureThrottler
import com.musicroom.server.common.websocket.configureWebSockets
import com.musicroom.server.config.AppConfig
import com.musicroom.server.config.ServiceRole
import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.auth.AuthModule
import com.musicroom.server.modules.applemusic.AppleMusicModule
import com.musicroom.server.modules.catalog.MusicProviderRegistry
import com.musicroom.server.modules.events.EventsModule
import com.musicroom.server.modules.playlists.PlaylistsModule
import com.musicroom.server.modules.premium.PremiumModule
import com.musicroom.server.modules.premium.stripe.StripeApi
import com.musicroom.server.modules.premium.stripe.StripeHttpApi
import com.musicroom.server.modules.social.PostgresSocialRelay
import com.musicroom.server.modules.social.SocialModule
import com.musicroom.server.modules.spotify.SpotifyModule
import com.musicroom.server.modules.spotify.client.SpotifyApi
import com.musicroom.server.modules.spotify.client.SpotifyHttpApi
import com.musicroom.server.modules.system.SystemModule
import com.musicroom.server.modules.users.UsersModule
import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing

class AppModule(
    config: AppConfig,
    database: DatabaseModule,
    passwordHasher: PasswordHasher = PasswordHasher(),
    emailSender: EmailSender = if (config.mail.host.isEmpty()) LogEmailSender() else SmtpEmailSender(config.mail),
    oauthVerifier: OAuthTokenVerifier = JwksOAuthTokenVerifier(config.oauth),
    spotifyApi: SpotifyApi = SpotifyHttpApi(config.spotify),
    stripeApi: StripeApi = StripeHttpApi(config.stripe),
) {
    private val secretBox = SecretBox(config.jwt.secret)
    val jwtService = JwtService(config.jwt)
    val users = UsersModule(database)
    val auth = AuthModule(
        database = database,
        users = users.repository,
        jwtService = jwtService,
        passwordHasher = passwordHasher,
        emailSender = emailSender,
        oauthVerifier = oauthVerifier,
        secretBox = secretBox,
        refreshTokenTtlDays = config.jwt.refreshTokenTtlDays,
    )

    val spotify = SpotifyModule(database, users.repository, config.spotify, spotifyApi, jwtService, secretBox)
    val appleMusic = AppleMusicModule(config.appleMusic)

    private val providers = MusicProviderRegistry(spotify.service, appleMusic.catalog)

    val role = config.role
    val trustProxy = config.trustProxy
    val socialRelay = PostgresSocialRelay(database, config.database)

    val social = SocialModule(database, users.repository, jwtService, socialRelay.takeIf { role.relaysSocialEvents })

    val premium = PremiumModule(database, users.repository, config.stripe, stripeApi)

    val events = EventsModule(database, providers, social.service, jwtService)

    private val system = SystemModule(config, database)
    private val playlists = PlaylistsModule(database, users.repository, providers, premium.tracks, social.service)

    val modules: List<Module> = when (role) {
        ServiceRole.ALL -> listOf(system, users, auth, spotify, appleMusic, playlists, social, events, premium)
        ServiceRole.IDENTITY -> listOf(system, users, auth)
        ServiceRole.MUSIC -> listOf(system, spotify, appleMusic)
        ServiceRole.LIBRARY -> listOf(system, playlists, premium)
        ServiceRole.REALTIME -> listOf(system, social, events)
    }
}

fun Application.bootstrap(appModule: AppModule) {
    configureLoggingInterceptor()
    configureSerialization()
    configureHttpMiddleware()
    configureExceptionFilter()
    configureJwtGuard(appModule.jwtService)
    configureThrottler(appModule.trustProxy)
    configureWebSockets()
    if (appModule.role.runsRealtime) {
        // Plays the events' music on their owners' Spotify until the server stops.
        appModule.events.service.player.run(this)
    }
    if (appModule.role == ServiceRole.REALTIME) {
        appModule.socialRelay.listen(this, appModule.social.hub::deliver)
    }

    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        appModule.modules.flatMap { it.controllers }.forEach { register(it) }
    }
}
