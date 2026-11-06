package com.musicroom.server.modules.playlists

import com.musicroom.server.common.Controller
import com.musicroom.server.common.Module
import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.premium.HostedTracksService
import com.musicroom.server.modules.spotify.SpotifyService
import com.musicroom.server.modules.users.UsersRepository
import com.musicroom.server.modules.applemusic.AppleMusicCatalog

class PlaylistsModule(
    database: DatabaseModule,
    users: UsersRepository,
    spotify: SpotifyService,
    hosted: HostedTracksService,
    friends: PlaylistFriends,
    appleMusic: AppleMusicCatalog,
) : Module {
    val service = PlaylistsService(
        PlaylistsRepository(database),
        PlaylistMembersRepository(database),
        PlaylistTracksRepository(database),
        users,
        spotify,
        hosted,
        friends,
        appleMusic,
    )

    override val controllers: List<Controller> = listOf(PlaylistsController(service))
}
