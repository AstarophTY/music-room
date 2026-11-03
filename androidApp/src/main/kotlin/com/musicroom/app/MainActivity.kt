package com.musicroom.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.musicroom.shared.ClientInfo
import com.musicroom.shared.MusicRoomApi
import io.ktor.client.engine.okhttp.OkHttp

class MainActivity : ComponentActivity() {
    private lateinit var api: MusicRoomApi

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val clientInfo = ClientInfo(
            platform = "android",
            device = "${Build.MANUFACTURER} ${Build.MODEL}",
            appVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown",
        )
        api = MusicRoomApi(AppEnv.BACKEND_URL, clientInfo, OkHttp, AndroidTokenStore(this))
        val socialSignIn = AndroidSocialSignIn(this, AppEnv.GOOGLE_CLIENT_ID)
        val settings = AndroidAppSettings(this)

        setContent {
            App(
                api = api,
                socialSignIn = socialSignIn,
                settings = settings,
                // The app can be dark while the phone is light (and the reverse): match the system bar icons.
                onDarkThemeChange = { dark ->
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
                },
            )
        }
    }

    override fun onDestroy() {
        api.close()
        super.onDestroy()
    }
}
