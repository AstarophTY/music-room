package com.musicroom.server.modules.system

import com.musicroom.shared.dto.HealthResponse
import com.musicroom.shared.dto.InfoResponse

class SystemService(
    private val version: String,
    private val isDatabaseUp: () -> Boolean,
) {
    fun health(): HealthResponse {
        val dbUp = isDatabaseUp()
        return HealthResponse(status = if (dbUp) "ok" else "degraded", database = if (dbUp) "up" else "down")
    }

    fun info() = InfoResponse(name = "music-room", version = version)
}
