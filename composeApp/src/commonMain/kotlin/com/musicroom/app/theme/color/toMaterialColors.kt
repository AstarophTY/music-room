package com.musicroom.app.theme.color

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

internal fun AppColorScheme.toMaterialColors(dark: Boolean) =
    (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = accent, onPrimary = onAccent, primaryContainer = surfaceSubtle,
        onPrimaryContainer = textPrimary, secondary = textSecondary, onSecondary = surface,
        secondaryContainer = surfaceSubtle, onSecondaryContainer = textPrimary,
        tertiary = accent, onTertiary = onAccent, tertiaryContainer = surfaceSubtle,
        onTertiaryContainer = textPrimary, background = background, onBackground = textPrimary,
        surface = surface, onSurface = textPrimary, surfaceVariant = surfaceSubtle,
        onSurfaceVariant = textSecondary, surfaceTint = surface, outline = borderStrong,
        outlineVariant = border, error = error, onError = onAccent,
        errorContainer = surfaceSubtle, onErrorContainer = error,
        inverseSurface = textPrimary, inverseOnSurface = surface, inversePrimary = accent,
        surfaceContainerLowest = surface, surfaceContainerLow = surface,
        surfaceContainer = surface, surfaceContainerHigh = surfaceSubtle,
        surfaceContainerHighest = surfaceSubtle, surfaceBright = surface, surfaceDim = surface,
    )
