package com.musicroom.app

import com.musicroom.app.settings.AppSettings
import com.musicroom.app.theme.ThemeMode
import platform.Foundation.NSUserDefaults

internal class IosAppSettings : AppSettings {
    private val defaults = NSUserDefaults.standardUserDefaults
    override var providerChoiceSeen: Boolean
        get() = defaults.boolForKey("provider_choice_seen")
        set(value) = defaults.setBool(value, forKey = "provider_choice_seen")

    override var themeMode: ThemeMode
        get() = defaults.stringForKey(THEME_KEY)?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } }
            ?: ThemeMode.System
        set(value) = defaults.setObject(value.name, forKey = THEME_KEY)

    private companion object {
        const val THEME_KEY = "theme_mode"
    }
}
