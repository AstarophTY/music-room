package com.musicroom.server.modules.premium

import com.musicroom.server.common.Controller
import com.musicroom.server.common.Module
import com.musicroom.server.config.StripeConfig
import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.modules.premium.stripe.StripeApi
import com.musicroom.server.modules.users.UsersRepository

class PremiumModule(database: DatabaseModule, users: UsersRepository, config: StripeConfig, api: StripeApi) : Module {
    val billing = BillingService(config, api, SubscriptionsRepository(database), users)
    val tracks = HostedTracksService(HostedTracksRepository(database), billing, users)

    override val controllers: List<Controller> = listOf(PremiumController(billing, tracks))
}
