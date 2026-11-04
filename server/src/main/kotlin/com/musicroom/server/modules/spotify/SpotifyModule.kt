package com.musicroom.server.modules.spotify

import com.musicroom.server.common.Controller
import com.musicroom.server.common.Module
import com.musicroom.server.common.jwt.JwtService
import com.musicroom.server.common.security.SecretBox
import com.musicroom.server.config.SpotifyConfig
import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.spotify.client.SpotifyApi
import com.musicroom.server.modules.users.UsersRepository

class SpotifyModule(
    database: DatabaseModule,
    users: UsersRepository,
    config: SpotifyConfig,
    api: SpotifyApi,
    jwtService: JwtService,
    secretBox: SecretBox,
) : Module {
    val accounts = SpotifyAccountsRepository(database, secretBox)
    val service = SpotifyService(config, api, accounts, jwtService)

    override val controllers: List<Controller> = listOf(SpotifyController(service, users))
}
