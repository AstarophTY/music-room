package com.musicroom.server.modules.applemusic

import com.musicroom.server.common.Controller
import com.musicroom.server.common.Module
import com.musicroom.server.config.AppleMusicConfig

class AppleMusicModule(config: AppleMusicConfig) : Module {
    private val tokens = AppleMusicTokenService(config)
    val catalog = AppleMusicCatalog(tokens)
    override val controllers: List<Controller> = listOf(AppleMusicController(tokens, catalog))
}
