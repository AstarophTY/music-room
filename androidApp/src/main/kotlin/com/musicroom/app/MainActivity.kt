package com.musicroom.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.musicroom.shared.ClientInfo
import com.musicroom.shared.MusicRoomApi
import io.ktor.client.engine.okhttp.OkHttp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val clientInfo = ClientInfo(
            platform = "android",
            device = "${Build.MANUFACTURER} ${Build.MODEL}",
            appVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown",
        )
        val settings = AndroidSettingsStore(this)

        setContent {
            App(
                settings = settings,
                apiFactory = { baseUrl -> MusicRoomApi(baseUrl, clientInfo, OkHttp) },
            )
        }
    }
}

