package com.musicroom.server.modules.playlists

import com.musicroom.server.common.Controller
import com.musicroom.server.common.Module
import com.musicroom.server.common.jwt.JwtService
import com.musicroom.server.config.DatabaseConfig
import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.database.PostgresListener
import com.musicroom.server.modules.premium.HostedTracksService
import com.musicroom.server.modules.users.UsersRepository
import com.musicroom.server.modules.catalog.MusicProviderRegistry
import io.ktor.server.application.Application
import java.util.UUID

class PlaylistsModule(
    database: DatabaseModule,
    users: UsersRepository,
    providers: MusicProviderRegistry,
    hosted: HostedTracksService,
    friends: PlaylistFriends,
    jwt: JwtService,
    private val databaseConfig: DatabaseConfig,
    /** Several replicas serve playlists: changes go through PostgreSQL to reach all their WebSockets. */
    relayChanges: Boolean = false,
) : Module {
    val service = PlaylistsService(
        PlaylistsRepository(database),
        PlaylistMembersRepository(database),
        PlaylistTracksRepository(database),
        users,
        providers.catalog,
        hosted,
        friends,
        publishChange = if (relayChanges) { id -> database.notify(CHANNEL, id.toString()) } else null,
    )

    override val controllers: List<Controller> = listOf(PlaylistsController(service, jwt))

    /** Pushes the changes made on any replica to the WebSockets held here. */
    fun listen(application: Application) {
        PostgresListener(databaseConfig, CHANNEL).listen(application) { service.hub.schedule(UUID.fromString(it)) }
    }

    private companion object {
        const val CHANNEL = "playlist_changes"
    }
}
