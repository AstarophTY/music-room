package com.musicroom.server.common.websocket

import com.musicroom.shared.MusicRoomJson
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlin.time.Duration.Companion.seconds

fun Application.configureWebSockets() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 30.seconds
        maxFrameSize = 64 * 1024L
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(MusicRoomJson)
    }
}
