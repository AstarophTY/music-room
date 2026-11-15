package com.musicroom.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.musicroom.app.common.HostBackendUrl
import com.musicroom.app.music.AppleMusicConnection
import com.musicroom.app.music.AppleMusicPlayback
import com.musicroom.shared.ClientInfo
import com.musicroom.shared.MusicRoomApi
import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

fun MainViewController() = ComposeUIViewController {
    val api = remember {
        val device = UIDevice.currentDevice
        val clientInfo = ClientInfo(
            platform = "ios",
            device = "${device.systemName} ${device.model}",
            appVersion = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "unknown",
        )
        MusicRoomApi(HostBackendUrl.value, clientInfo, Darwin, IosTokenStore())
    }
    val appleMusic = remember(api) { IosAppleMusicConnection(api) }
    val applePlayback = remember(appleMusic) { IosAppleMusicPlayback(appleMusic) }
    CompositionLocalProvider(AppleMusicConnection.Local provides appleMusic, AppleMusicPlayback.Local provides applePlayback) {
        App(api, settings = remember { IosAppSettings() })
    }
}
