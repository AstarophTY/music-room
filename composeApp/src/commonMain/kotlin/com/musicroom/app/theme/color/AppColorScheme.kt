package com.musicroom.app.theme.color

import androidx.compose.ui.graphics.Color

data class AppColorScheme(
    val background: Color,
    val surface: Color,
    val surfaceSubtle: Color,
    val border: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    /** Brand yellow: primary buttons, highlights. */
    val accent: Color,
    val onAccent: Color,
    /** Soft yellow backgrounds (avatars, chips, selected items). */
    val accentSubtle: Color,
    val accentSubtleStrong: Color,
    val accentBorder: Color,
    /** Yellow that stays readable as text on the background. */
    val accentText: Color,
    val error: Color,
    val success: Color,
    val warning: Color,
)
