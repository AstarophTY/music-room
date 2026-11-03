package com.musicroom.app

import android.content.Context
import androidx.core.content.edit
import com.musicroom.shared.TokenStore

internal class AndroidTokenStore(context: Context) : TokenStore {
    private val prefs = context.getSharedPreferences("music_room", Context.MODE_PRIVATE)

    override var refreshToken: String?
        get() = prefs.getString(KEY, null)
        set(value) = prefs.edit { if (value == null) remove(KEY) else putString(KEY, value) }

    private companion object {
        const val KEY = "refresh_token"
    }
}
