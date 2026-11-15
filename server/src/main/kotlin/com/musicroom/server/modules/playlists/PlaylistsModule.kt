package com.musicroom.server.modules.playlists

import com.musicroom.server.common.Controller
import com.musicroom.server.common.Module
import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.premium.HostedTracksService
import com.musicroom.server.modules.users.UsersRepository
import com.musicroom.server.modules.catalog.MusicProviderRegistry

class PlaylistsModule(
    database: DatabaseModule,
    users: UsersRepository,
    providers: MusicProviderRegistry,
    hosted: HostedTracksService,
    friends: PlaylistFriends,
) : Module {
    val service = PlaylistsService(
        PlaylistsRepository(database),
        PlaylistMembersRepository(database),
        PlaylistTracksRepository(database),
        users,
        providers.catalog,
        hosted,
        friends,
    )

    override val controllers: List<Controller> = listOf(PlaylistsController(service))
}
