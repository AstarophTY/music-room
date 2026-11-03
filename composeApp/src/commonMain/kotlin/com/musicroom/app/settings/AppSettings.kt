package com.musicroom.app.settings

import com.musicroom.app.theme.ThemeMode

/** User preferences kept on the device. */
interface AppSettings {
    var themeMode: ThemeMode

    /** Not saved: used where no platform storage is wired. */
    class InMemory(override var themeMode: ThemeMode = ThemeMode.System) : AppSettings
}
