package com.musicroom.server.modules.users

import com.musicroom.server.common.Controller
import com.musicroom.server.common.Module
import com.musicroom.server.database.DatabaseModule

class UsersModule(database: DatabaseModule) : Module {
    val repository = UsersRepository(database)
    val service = UsersService(repository, AvatarsRepository(database))

    override val controllers: List<Controller> = listOf(UsersController(service))
}
