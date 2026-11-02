package com.musicroom.app

import android.content.Context
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
            appVersion = BuildConfig.VERSION_NAME,
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

private class AndroidSettingsStore(context: Context) : SettingsStore {
    private val prefs = context.getSharedPreferences("music_room", Context.MODE_PRIVATE)

    override var backendUrl: String
        get() = prefs.getString("backend_url", null) ?: "http://10.0.2.2:8080"
        set(value) = prefs.edit().putString("backend_url", value).apply()
}
