package com.musicroom.server.modules.social

import com.musicroom.server.common.Controller
import com.musicroom.server.common.Module
import com.musicroom.server.common.jwt.JwtService
import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.users.UsersRepository

class SocialModule(
    database: DatabaseModule,
    users: UsersRepository,
    jwt: JwtService,
    relay: SocialRelay? = null,
) : Module {
    val hub = SocialHub(relay)
    val service = SocialService(
        PeopleRepository(database),
        FriendshipsRepository(database),
        MessagesRepository(database),
        NotificationsRepository(database),
        users,
        hub,
    )

    override val controllers: List<Controller> = listOf(SocialController(service, hub, jwt))
}
