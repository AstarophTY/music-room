package com.musicroom.app

import com.musicroom.shared.TokenStore
import platform.Foundation.NSUserDefaults

internal class IosTokenStore : TokenStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override var refreshToken: String?
        get() = defaults.stringForKey(KEY)
        set(value) = if (value == null) defaults.removeObjectForKey(KEY) else defaults.setObject(value, forKey = KEY)

    private companion object {
        const val KEY = "refresh_token"
    }
}
