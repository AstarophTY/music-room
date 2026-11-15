package com.musicroom.app

import android.content.Context
import androidx.core.content.edit
import com.musicroom.app.settings.AppSettings
import com.musicroom.app.theme.ThemeMode

internal class AndroidAppSettings(context: Context) : AppSettings {
    private val prefs = context.getSharedPreferences("music_room_settings", Context.MODE_PRIVATE)
    override var providerChoiceSeen: Boolean
        get() = prefs.getBoolean("provider_choice_seen", false)
        set(value) = prefs.edit { putBoolean("provider_choice_seen", value) }

    override var themeMode: ThemeMode
        get() = prefs.getString(THEME_KEY, null)?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } }
            ?: ThemeMode.System
        set(value) = prefs.edit { putString(THEME_KEY, value.name) }

    private companion object {
        const val THEME_KEY = "theme_mode"
    }
}
