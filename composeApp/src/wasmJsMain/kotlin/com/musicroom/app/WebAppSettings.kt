package com.musicroom.app

import com.musicroom.app.settings.AppSettings
import com.musicroom.app.theme.ThemeMode
import kotlinx.browser.localStorage

internal class WebAppSettings : AppSettings {
    override var themeMode: ThemeMode
        get() = localStorage.getItem(THEME_KEY)?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } }
            ?: ThemeMode.System
        set(value) = localStorage.setItem(THEME_KEY, value.name)

    private companion object {
        const val THEME_KEY = "music-room.theme_mode"
    }
}
