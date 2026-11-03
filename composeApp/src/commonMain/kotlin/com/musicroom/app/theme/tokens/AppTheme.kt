package com.musicroom.app.theme.tokens

import com.musicroom.app.theme.color.AppColorScheme
import com.musicroom.app.theme.color.LocalAppColors
import com.musicroom.app.theme.typography.AppTypography
import com.musicroom.app.theme.LocalTypography

import androidx.compose.runtime.Composable

object AppTheme {
    val colors: AppColorScheme @Composable get() = LocalAppColors.current
    val spacing: AppSpacing @Composable get() = LocalAppSpacing.current
    val typography: AppTypography @Composable get() = LocalTypography.current
}

