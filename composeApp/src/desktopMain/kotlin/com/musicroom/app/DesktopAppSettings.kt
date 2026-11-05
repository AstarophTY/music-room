package com.musicroom.app

import com.musicroom.app.settings.AppSettings
import com.musicroom.app.theme.ThemeMode
import java.util.prefs.Preferences

internal class DesktopAppSettings(private val prefs: Preferences) : AppSettings {
    override var providerChoiceSeen: Boolean
        get() = prefs.getBoolean("provider_choice_seen", false)
        set(value) = prefs.putBoolean("provider_choice_seen", value)
    override var themeMode: ThemeMode
        get() = prefs.get(THEME_KEY, null)?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } }
            ?: ThemeMode.System
        set(value) = prefs.put(THEME_KEY, value.name)

    private companion object {
        const val THEME_KEY = "theme_mode"
    }
}
