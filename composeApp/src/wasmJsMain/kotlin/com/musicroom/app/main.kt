package com.musicroom.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.musicroom.app.common.HostBackendUrl
import com.musicroom.shared.ClientInfo
import com.musicroom.shared.MusicRoomApi
import io.ktor.client.engine.js.Js
import kotlinx.browser.document
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val clientInfo = ClientInfo(platform = "web", device = window.navigator.platform.ifEmpty { "browser" }, appVersion = "0.1.0")
    val api = MusicRoomApi(HostBackendUrl.value, clientInfo, Js, WebTokenStore())
    ComposeViewport(document.body!!) {
        App(api, settings = WebAppSettings())
    }
}
