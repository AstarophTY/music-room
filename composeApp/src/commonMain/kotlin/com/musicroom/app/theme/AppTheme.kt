package com.musicroom.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.LocalTonalElevationEnabled
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.musicroom.app.theme.color.DarkColors
import com.musicroom.app.theme.color.LightColors
import com.musicroom.app.theme.color.LocalAppColors
import com.musicroom.app.theme.color.toMaterialColors
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppSpacing
import com.musicroom.app.theme.tokens.LocalAppSpacing
import com.musicroom.app.theme.typography.AppTypography
import com.musicroom.app.theme.typography.createAppTypography

internal val LocalTypography = staticCompositionLocalOf<AppTypography> { error("AppTheme is required") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (dark) DarkColors else LightColors
    val typography = createAppTypography()
    CompositionLocalProvider(
        LocalAppColors provides colors, LocalAppSpacing provides AppSpacing,
        LocalTypography provides typography, LocalRippleConfiguration provides null,
        LocalTonalElevationEnabled provides false,
    ) {
        MaterialTheme(colorScheme = colors.toMaterialColors(dark), typography = typography.material(), shapes = AppShapes.material, content = content)
    }
}
