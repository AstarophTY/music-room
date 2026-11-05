package com.musicroom.app

import com.musicroom.shared.TokenStore
import kotlinx.browser.localStorage

/**
 * Keeps the refresh token in the browser's localStorage. Any script running on the page can read it,
 * so the web app must never load untrusted scripts. (An HttpOnly cookie would be safer for production.)
 */
internal class WebTokenStore : TokenStore {
    override var refreshToken: String?
        get() = localStorage.getItem(KEY)
        set(value) = if (value == null) localStorage.removeItem(KEY) else localStorage.setItem(KEY, value)

    private companion object {
        const val KEY = "music-room.refresh_token"
    }
}
