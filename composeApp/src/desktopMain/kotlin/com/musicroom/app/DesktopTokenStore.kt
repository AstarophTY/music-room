package com.musicroom.app

import com.musicroom.shared.TokenStore
import java.util.prefs.Preferences

/** Keeps the refresh token in the user's preferences (registry on Windows, plist on macOS, ~/.java on Linux). */
internal class DesktopTokenStore(private val prefs: Preferences) : TokenStore {
    override var refreshToken: String?
        get() = prefs.get(KEY, null)
        set(value) = if (value == null) prefs.remove(KEY) else prefs.put(KEY, value)

    private companion object {
        const val KEY = "refresh_token"
    }
}
