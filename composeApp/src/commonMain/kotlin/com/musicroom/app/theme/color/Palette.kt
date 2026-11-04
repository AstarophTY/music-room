package com.musicroom.app.theme.color

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.musicroom.shared.dto.AvatarColor

/** Radix Colors (radix-ui.com/colors): amber as the brand color, sand as its matching gray. */
private object Palette {
    // Amber, light theme
    val amber1 = Color(0xFFFEFDFB)
    val amber3 = Color(0xFFFFF7C2)
    val amber4 = Color(0xFFFFEE9C)
    val amber7 = Color(0xFFE9C162)
    val amber9 = Color(0xFFFFC53D)
    val amber11 = Color(0xFFAB6400)

    // Amber, dark theme
    val amberDark3 = Color(0xFF302008)
    val amberDark4 = Color(0xFF3F2700)
    val amberDark7 = Color(0xFF714F19)
    val amberDark11 = Color(0xFFFFCA16)

    // Sand, light theme
    val sand1 = Color(0xFFFDFDFC)
    val sand3 = Color(0xFFF1F0EF)
    val sand6 = Color(0xFFDAD9D6)
    val sand8 = Color(0xFFBCBBB5)
    val sand9 = Color(0xFF8D8D86)
    val sand11 = Color(0xFF63635E)
    val sand12 = Color(0xFF21201C)

    // Sand, dark theme
    val sandDark1 = Color(0xFF111110)
    val sandDark2 = Color(0xFF191918)
    val sandDark3 = Color(0xFF222221)
    val sandDark6 = Color(0xFF3B3A37)
    val sandDark8 = Color(0xFF62605B)
    val sandDark9 = Color(0xFF6F6D66)
    val sandDark11 = Color(0xFFB5B3AD)
    val sandDark12 = Color(0xFFEEEEEC)

    // Avatar colors: step 3 (background), 7 (border), 11 (text), light then dark.
    val tomato3 = Color(0xFFFEEBE7)
    val tomato7 = Color(0xFFF5A898)
    val tomato11 = Color(0xFFD13415)
    val tomatoDark3 = Color(0xFF391714)
    val tomatoDark7 = Color(0xFF853A2D)
    val tomatoDark11 = Color(0xFFFF977D)
    val pink3 = Color(0xFFFEE9F5)
    val pink7 = Color(0xFFE7ACD0)
    val pink11 = Color(0xFFC2298A)
    val pinkDark3 = Color(0xFF37172F)
    val pinkDark7 = Color(0xFF803D6B)
    val pinkDark11 = Color(0xFFFF8DCC)
    val iris3 = Color(0xFFF0F1FE)
    val iris7 = Color(0xFFB8BAF8)
    val iris11 = Color(0xFF5753C6)
    val irisDark3 = Color(0xFF202248)
    val irisDark7 = Color(0xFF4A4A95)
    val irisDark11 = Color(0xFFB1A9FF)
    val teal3 = Color(0xFFE0F8F3)
    val teal7 = Color(0xFF83CDC1)
    val teal11 = Color(0xFF008573)
    val tealDark3 = Color(0xFF0D2D2A)
    val tealDark7 = Color(0xFF1C6961)
    val tealDark11 = Color(0xFF0BD8B6)
    val grass3 = Color(0xFFE9F6E9)
    val grass7 = Color(0xFF94CE9A)
    val grass11 = Color(0xFF2A7E3B)
    val grassDark3 = Color(0xFF1B2A1E)
    val grassDark7 = Color(0xFF2D5736)
    val grassDark11 = Color(0xFF71D083)

    // Status (step 11 = readable text on the background)
    val red11 = Color(0xFFCE2C31)
    val redDark11 = Color(0xFFFF9592)
    val green11 = Color(0xFF218358)
    val greenDark11 = Color(0xFF3DD68C)
}

val LightColors = AppColorScheme(
    background = Palette.amber1,
    surface = Palette.sand1,
    surfaceSubtle = Palette.sand3,
    border = Palette.sand6,
    borderStrong = Palette.sand8,
    textPrimary = Palette.sand12,
    textSecondary = Palette.sand11,
    textDisabled = Palette.sand9,
    accent = Palette.amber9,
    // Radix: text on amber 9 must be dark, in both themes.
    onAccent = Palette.sand12,
    accentSubtle = Palette.amber3,
    accentSubtleStrong = Palette.amber4,
    accentBorder = Palette.amber7,
    accentText = Palette.amber11,
    error = Palette.red11,
    success = Palette.green11,
    warning = Palette.amber11,
    avatars = mapOf(
        AvatarColor.YELLOW to AvatarTint(Palette.amber4, Palette.amber7, Palette.amber11),
        AvatarColor.TOMATO to AvatarTint(Palette.tomato3, Palette.tomato7, Palette.tomato11),
        AvatarColor.PINK to AvatarTint(Palette.pink3, Palette.pink7, Palette.pink11),
        AvatarColor.IRIS to AvatarTint(Palette.iris3, Palette.iris7, Palette.iris11),
        AvatarColor.TEAL to AvatarTint(Palette.teal3, Palette.teal7, Palette.teal11),
        AvatarColor.GRASS to AvatarTint(Palette.grass3, Palette.grass7, Palette.grass11),
    ),
)

val DarkColors = AppColorScheme(
    background = Palette.sandDark1,
    surface = Palette.sandDark2,
    surfaceSubtle = Palette.sandDark3,
    border = Palette.sandDark6,
    borderStrong = Palette.sandDark8,
    textPrimary = Palette.sandDark12,
    textSecondary = Palette.sandDark11,
    textDisabled = Palette.sandDark9,
    accent = Palette.amber9,
    onAccent = Palette.sand12,
    accentSubtle = Palette.amberDark3,
    accentSubtleStrong = Palette.amberDark4,
    accentBorder = Palette.amberDark7,
    accentText = Palette.amberDark11,
    error = Palette.redDark11,
    success = Palette.greenDark11,
    warning = Palette.amberDark11,
    avatars = mapOf(
        AvatarColor.YELLOW to AvatarTint(Palette.amberDark4, Palette.amberDark7, Palette.amberDark11),
        AvatarColor.TOMATO to AvatarTint(Palette.tomatoDark3, Palette.tomatoDark7, Palette.tomatoDark11),
        AvatarColor.PINK to AvatarTint(Palette.pinkDark3, Palette.pinkDark7, Palette.pinkDark11),
        AvatarColor.IRIS to AvatarTint(Palette.irisDark3, Palette.irisDark7, Palette.irisDark11),
        AvatarColor.TEAL to AvatarTint(Palette.tealDark3, Palette.tealDark7, Palette.tealDark11),
        AvatarColor.GRASS to AvatarTint(Palette.grassDark3, Palette.grassDark7, Palette.grassDark11),
    ),
)

val LocalAppColors = staticCompositionLocalOf { LightColors }
