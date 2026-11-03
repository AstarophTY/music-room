package com.musicroom.app

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
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
        MusicRoomApi(AppEnv.BACKEND_URL, clientInfo, Darwin, IosTokenStore())
    }
    App(api, settings = remember { IosAppSettings() })
}
