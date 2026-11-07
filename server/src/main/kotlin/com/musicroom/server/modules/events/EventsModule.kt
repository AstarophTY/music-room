package com.musicroom.server.modules.events

import com.musicroom.server.common.Controller
import com.musicroom.server.common.Module
import com.musicroom.server.common.jwt.JwtService
import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.social.PeopleRepository
import com.musicroom.server.modules.spotify.SpotifyService
import com.musicroom.server.modules.applemusic.AppleMusicCatalog

class EventsModule(database: DatabaseModule, spotify: SpotifyService, friends: EventFriends, jwt: JwtService, appleMusic: AppleMusicCatalog) : Module {
    val service = EventsService(
        EventsRepository(database),
        EventTracksRepository(database),
        PeopleRepository(database),
        spotify,
        friends,
        appleMusic,
    )

    override val controllers: List<Controller> = listOf(EventsController(service, jwt))
}
