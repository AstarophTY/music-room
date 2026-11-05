package com.musicroom.server.modules.playlists

import com.musicroom.server.common.Controller
import com.musicroom.server.common.Module
import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.spotify.SpotifyService
import com.musicroom.server.modules.users.UsersRepository

class PlaylistsModule(database: DatabaseModule, users: UsersRepository, spotify: SpotifyService, friends: PlaylistFriends) : Module {
    val service = PlaylistsService(
        PlaylistsRepository(database),
        PlaylistMembersRepository(database),
        PlaylistTracksRepository(database),
        users,
        spotify,
        friends,
    )

    override val controllers: List<Controller> = listOf(PlaylistsController(service))
}
