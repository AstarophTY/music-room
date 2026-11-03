package com.musicroom.app

import android.content.Context
import androidx.core.content.edit

internal class AndroidSettingsStore(context: Context) : SettingsStore {
    private val prefs = context.getSharedPreferences("music_room", Context.MODE_PRIVATE)

    override var backendUrl: String
        get() = prefs.getString("backend_url", null) ?: "http://10.0.2.2:8080"
        set(value) = prefs.edit { putString("backend_url", value) }
}
