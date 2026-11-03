package com.musicroom.app.theme.tokens

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

object AppSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val border = 1.dp
}
val LocalAppSpacing = staticCompositionLocalOf { AppSpacing }
