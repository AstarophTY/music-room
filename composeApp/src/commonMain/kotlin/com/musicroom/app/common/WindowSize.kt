package com.musicroom.app.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Width class of the window (Material breakpoints): phone, tablet / small window, large screen. */
enum class WindowSize {
    Compact,
    Medium,
    Expanded,
    ;

    companion object {
        fun fromWidth(width: Dp): WindowSize = when {
            width < 600.dp -> Compact
            width < 840.dp -> Medium
            else -> Expanded
        }

        /** Provided once by App from the window width. */
        val Local: ProvidableCompositionLocal<WindowSize> = staticCompositionLocalOf { Compact }

        val current: WindowSize
            @Composable get() = Local.current
    }
}
