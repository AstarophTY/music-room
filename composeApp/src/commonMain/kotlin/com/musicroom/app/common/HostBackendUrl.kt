package com.musicroom.app.common

import com.musicroom.app.AppEnv

/**
 * Backend URL for apps running on the computer itself (desktop, web).
 * 10.0.2.2 is how the Android emulator reaches the computer; here it is simply localhost.
 */
object HostBackendUrl {
    val value: String = AppEnv.BACKEND_URL.replace("10.0.2.2", "localhost")
}
