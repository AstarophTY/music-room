package com.musicroom.app.theme.color

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private object Palette {
    val neutral0 = Color(0xFFFAF9F7)
    val neutral1 = Color(0xFFF1F0ED)
    val neutral2 = Color(0xFFDFDDD8)
    val neutral3 = Color(0xFFAAA8A2)
    val neutral4 = Color(0xFF716F6A)
    val neutral5 = Color(0xFF383735)
    val neutral6 = Color(0xFF242321)
    val neutral7 = Color(0xFF191816)
    val accent = Color(0xFF30302F)
    val accentDark = Color(0xFFE4E2DD)
    val error = Color(0xFFA04C48)
    val success = Color(0xFF507B62)
    val warning = Color(0xFF98733D)
}


val LightColors = AppColorScheme(
    Palette.neutral0, Palette.neutral0, Palette.neutral1, Palette.neutral2,
    Palette.neutral3, Palette.neutral7, Palette.neutral4, Palette.neutral3,
    Palette.accent, Palette.neutral0, Palette.error, Palette.success, Palette.warning,
)
val DarkColors = AppColorScheme(
    Palette.neutral7, Palette.neutral6, Palette.neutral5, Palette.neutral5,
    Palette.neutral4, Palette.neutral0, Palette.neutral3, Palette.neutral4,
    Palette.accentDark, Palette.neutral7, Palette.error, Palette.success, Palette.warning,
)
val LocalAppColors = staticCompositionLocalOf { LightColors }

