package com.musicroom.app

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.musicroom.app.common.HostBackendUrl
import com.musicroom.shared.ClientInfo
import com.musicroom.shared.MusicRoomApi
import io.ktor.client.engine.okhttp.OkHttp
import java.awt.Dimension
import java.util.prefs.Preferences

fun main() {
    val prefs = Preferences.userRoot().node("music-room")
    val clientInfo = ClientInfo(
        platform = "desktop",
        device = "${System.getProperty("os.name")} ${System.getProperty("os.arch")}",
        appVersion = "0.1.0",
    )
    val api = MusicRoomApi(HostBackendUrl.value, clientInfo, OkHttp, DesktopTokenStore(prefs))
    val settings = DesktopAppSettings(prefs)

    application {
        Window(
            onCloseRequest = {
                api.close()
                exitApplication()
            },
            title = "Music Room",
            state = rememberWindowState(size = DpSize(1280.dp, 820.dp)),
        ) {
            window.minimumSize = Dimension(380, 600)
            App(api, settings = settings)
        }
    }
}
