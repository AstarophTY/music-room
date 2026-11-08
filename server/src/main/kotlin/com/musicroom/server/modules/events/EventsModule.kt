package com.musicroom.server.modules.events

import com.musicroom.server.common.Controller
import com.musicroom.server.common.Module
import com.musicroom.server.common.jwt.JwtService
import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.social.PeopleRepository
import com.musicroom.server.modules.catalog.MusicProviderRegistry

class EventsModule(database: DatabaseModule, providers: MusicProviderRegistry, friends: EventFriends, jwt: JwtService) : Module {
    val service = EventsService(
        EventsRepository(database),
        EventTracksRepository(database),
        PeopleRepository(database),
        providers.catalog,
        providers.eventPlayback,
        friends,
    )

    override val controllers: List<Controller> = listOf(EventsController(service, jwt))
}
