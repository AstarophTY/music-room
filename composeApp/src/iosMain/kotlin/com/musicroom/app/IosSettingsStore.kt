package com.musicroom.app

import platform.Foundation.NSUserDefaults

internal class IosSettingsStore : SettingsStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override var backendUrl: String
        get() = defaults.stringForKey("backend_url") ?: "http://localhost:8080"
        set(value) = defaults.setObject(value, forKey = "backend_url")
}
